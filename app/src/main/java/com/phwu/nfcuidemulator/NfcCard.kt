package com.phwu.nfcuidemulator

import java.io.Serializable

data class NfcCard(var id: Long, val name: String, val uid: String) : Serializable {

    companion object {
        val ID_EMPTY_CARD = -1L
        val ID_DEFAULT_CARD = 0L

        val TABLE_NAME = "cards"

        val COLUMN_ID = "id"
        val COLUMN_NAME = "name"
        val COLUMN_UID = "uid"
    }

}