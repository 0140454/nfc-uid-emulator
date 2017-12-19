package com.phwu.nfcuidemulator

import java.io.File
import java.io.FileNotFoundException

object NfcConfig {

    private val configPath = getConfigPath()
    private val data = load()

    fun get(name: String): String? {
        return data[name]
    }

    fun set(name: String, value: String) {
        data[name] = value
    }

    fun save(): Boolean {
        val content = data.toList().joinToString("\n") { (key, value) -> "$key=$value" }
        val shell = Runtime.getRuntime().exec("su")
        val shellWriter = shell.outputStream.bufferedWriter()

        with(shellWriter) {
            write("mount -o rw,remount $(stat -c '%m' $configPath)\n")
            if (!File("$configPath.orig").exists()) {
                write("cp $configPath $configPath.orig\n")
            }
            write("echo -n '$content' > $configPath\n")
            write("mount -o ro,remount $(stat -c '%m' $configPath)\n")
            write("exit\n")
            flush()
        }

        shell.waitFor()
        shellWriter.close()

        return File(configPath).readText() == content
    }

    fun restore(): Boolean {
        if (!File("$configPath.orig").exists()) {
            throw FileNotFoundException()
        }

        val shell = Runtime.getRuntime().exec("su")
        val shellWriter = shell.outputStream.bufferedWriter()

        with(shellWriter) {
            write("mount -o rw,remount $(stat -c '%m' $configPath)\n")
            write("cp $configPath.orig $configPath\n")
            write("mount -o ro,remount $(stat -c '%m' $configPath)\n")
            write("exit\n")
            flush()
        }

        shell.waitFor()
        shellWriter.close()

        return File(configPath).readText() == File(configPath + ".orig").readText()
    }

    fun isNxpController(): Boolean {
        return configPath.contains("nxp")
    }

    private fun load(): MutableMap<String, String> {
        val result: MutableMap<String, String> = HashMap()
        val content = File(configPath).readText()

        Regex(
                "^\\s*([^#\\s]+)\\s*=\\s*((?:\\{.*?\\})|(?:\".*?\")|(?:0[xX][0-9a-fA-F]+|[0-9]+))\\s*$",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        ).findAll(content).map { match ->
            match.groups[1]!!.value to match.groups[2]!!.value.replace(Regex("\\s"), "")
        }.toMap(result)

        return result
    }

    private fun getConfigPath(): String {
        val shell: Process = Runtime.getRuntime().exec("sh")
        val shellWriter = shell.outputStream.bufferedWriter()
        val shellReader = shell.inputStream.bufferedReader()

        shellWriter.write("strings $(ls /system/lib*/*nfc*jni*) | grep libnfc.*\\.conf$\n")
        shellWriter.flush()
        val fileName = shellReader.readLine()

        shellWriter.write("strings $(ls /system/lib*/*nfc*jni*) | grep etc/$\n")
        shellWriter.write("exit\n")
        shellWriter.flush()
        shell.waitFor()

        val result = shellReader.lineSequence().filter { etcPath ->
            File("$etcPath$fileName").exists()
        }.map { path ->
            path + fileName
        }.single()

        shellReader.close()
        shellWriter.close()

        return result
    }

}
