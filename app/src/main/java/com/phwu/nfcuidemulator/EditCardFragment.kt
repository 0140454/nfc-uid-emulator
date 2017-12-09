package com.phwu.nfcuidemulator

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import android.widget.EditText
import kotlinx.android.synthetic.main.fragment_edit_card.view.*
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.toast

class EditCardFragment : DialogFragment() {

    private var editCardView: View? = null
    private var callback: ((NfcCard) -> Unit?)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        editCardView = activity.layoutInflater.inflate(R.layout.fragment_edit_card, null)

        val dialog = AlertDialog.Builder(activity)
                .setView(editCardView)
                .setTitle(getString(R.string.edit_card_title))
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        dialog.setOnShowListener { _ ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
                val card = generateNfcCard()
                if (card == null) {
                    toast(getString(R.string.edit_card_invalid_data))
                } else {
                    if (callback != null) {
                        callback?.invoke(card)
                    }

                    dialog.dismiss()
                }
            }
        }

        val card = getArgument()
        if (card is NfcCard) {
            editCardView!!.edit_card_name.setText(card.name)
            for ((i, byte) in card.uid.split(":").withIndex()) {
                val editText = editCardView!!.edit_card_bytes.getChildAt(i)
                if (editText is EditText) {
                    editText.visibility = View.VISIBLE
                    editText.setText(byte)
                }
            }
        }

        return dialog
    }

    fun setCallback(callback: (NfcCard) -> Unit) {
        this.callback = callback
    }

    private fun getArgument(): NfcCard? {
        if (arguments != null) {
            val card = arguments["card"]
            if (card is NfcCard) {
                return card
            }
        }

        return null
    }

    private fun generateNfcCard(): NfcCard? {
        var id = NfcCard.ID_EMPTY_CARD
        val name = editCardView!!.edit_card_name.text.toString()
        if (name.isEmpty()) {
            return null
        }

        val uidBytes = editCardView?.edit_card_bytes!!.childrenSequence().filter { view ->
            view.visibility == View.VISIBLE && view is EditText
        }.map { view ->
            (view as EditText).text
        }
        if (uidBytes.any { str -> str.isEmpty() }) {
            return null
        }

        val uid = uidBytes.joinToString(":") { str -> str.padStart(2, '0') }

        val card = getArgument()
        if (card is NfcCard) {
            id = card.id
        }

        return NfcCard(id, name, uid)
    }

}