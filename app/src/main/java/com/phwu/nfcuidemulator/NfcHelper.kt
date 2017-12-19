package com.phwu.nfcuidemulator

import android.content.ContentValues.TAG
import android.content.Context
import android.nfc.NfcAdapter
import android.util.Log
import org.jetbrains.anko.getStackTraceString
import java.lang.reflect.Method


class NfcHelper(private val applicationContext: Context) {

    companion object {
        val NFCA_PARAM_LA_NFCID1 = 0x33

        val NIC_CONF_FIELD = if (NfcConfig.isNxpController()) "NXP_CORE_CONF" else "NFA_DM_START_UP_CFG"
        val ARRAY_SEPARATOR = if (NfcConfig.isNxpController()) "," else ":"
    }

    fun startService(): Boolean {
        return enableNFC(true)
    }

    fun stopService(): Boolean {
        return enableNFC(false)
    }

    fun restoreConfiguration(): Boolean {
        return NfcConfig.restore()
    }

    fun getUid(): String? {
        val data = NfcConfig.get(NIC_CONF_FIELD)
        if (data != null) {
            var offset = if (NfcConfig.isNxpController()) 4 else 1
            val dataBytes = data.removeSurrounding("{", "}").split(ARRAY_SEPARATOR)
                    .map { str -> str.toInt(16) }.toMutableList()

            while (offset < dataBytes.size && dataBytes[offset] != NFCA_PARAM_LA_NFCID1) {
                offset += 2 + dataBytes[offset + 1]
            }

            if (offset >= dataBytes.size) {
                return ""
            }

            return dataBytes.subList(offset + 2, offset + 2 + dataBytes[offset + 1])
                    .joinToString(":") { byte ->
                        byte.toString(16).padStart(2, '0')
                    }
        }

        return null
    }

    fun setUid(uid: String): Boolean {
        val data = NfcConfig.get(NIC_CONF_FIELD)
        if (data != null) {
            var offset = if (NfcConfig.isNxpController()) 4 else 1
            val dataBytes = data.removeSurrounding("{", "}").split(ARRAY_SEPARATOR)
                    .map { str -> str.toInt(16) }.toMutableList()
            val uidBytes = if (uid.isEmpty()) ArrayList() else uid.split(":")
                    .map { str -> str.toInt(16) }

            while (offset < dataBytes.size && dataBytes[offset] != NFCA_PARAM_LA_NFCID1) {
                offset += 2 + dataBytes[offset + 1]
            }

            val origUidLength = if (offset < dataBytes.size) dataBytes[offset + 1] else -1
            if (origUidLength == -1) {
                dataBytes[if (NfcConfig.isNxpController()) 2 else 0] += 2 + uidBytes.size
                dataBytes.addAll(arrayOf(NFCA_PARAM_LA_NFCID1, uidBytes.size) + uidBytes)
            } else {
                dataBytes[if (NfcConfig.isNxpController()) 2 else 0] += uidBytes.size - origUidLength
                dataBytes[offset + 1] = uidBytes.size

                for (counter in 0 until origUidLength) {
                    dataBytes.removeAt(offset + 2)
                }

                dataBytes.addAll(offset + 2, uidBytes)
            }

            val newData = "{" + dataBytes.joinToString(ARRAY_SEPARATOR) { byte ->
                byte.toString(16).toUpperCase().padStart(2, '0')
            } + "}"

            NfcConfig.set(NIC_CONF_FIELD, newData)
            return NfcConfig.save()
        }

        return false
    }

    private fun grantSecureSettingsPermission() {
        val shell = Runtime.getRuntime().exec("su")
        val shellWriter = shell.outputStream.bufferedWriter()

        with(shellWriter) {
            write("pm grant ${applicationContext.packageName} android.permission.WRITE_SECURE_SETTINGS\n")
            write("exit\n")
            flush()
        }

        shell.waitFor()
        shellWriter.close()
    }

    private fun enableNFC(enabled: Boolean): Boolean {
        grantSecureSettingsPermission()

        val adapter = NfcAdapter.getDefaultAdapter(applicationContext)
        if (adapter != null) {
            val nfcManagerClass: Class<*>
            val nfcControlMethod: Method

            try {
                nfcManagerClass = Class.forName(adapter.javaClass.name)

                nfcControlMethod = nfcManagerClass.getDeclaredMethod(
                        if (enabled) "enable" else "disable"
                )
                nfcControlMethod.isAccessible = true

                return nfcControlMethod.invoke(adapter) as Boolean
            } catch (e: Exception) {
                Log.e(TAG, e.getStackTraceString())
            }
        }

        return false
    }

}