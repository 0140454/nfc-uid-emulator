package com.phwu.nfcuidemulator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView

class NfcCardArrayAdapter(context: Context, resource: Int, items: MutableList<NfcCard>) :
        ArrayAdapter<NfcCard>(context, resource, items) {

    private var currentUid = ""

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(
                R.layout.simple_list_item_2_single_choice,
                parent,
                false
        )

        val card = getItem(position)
        val title = view.findViewById<TextView>(android.R.id.text1)
        val desc = view.findViewById<TextView>(android.R.id.text2)
        val radio = view.findViewById<RadioButton>(R.id.radio)

        title.text = card.name
        desc.text = card.uid
        radio.isChecked = currentUid == card.uid

        return view
    }

    fun setCurrentCard(uid: String) {
        currentUid = uid
        notifyDataSetChanged()
    }

}
