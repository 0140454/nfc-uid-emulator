package com.phwu.nfcuidemulator

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class NfcProfileDatabaseHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "saved_cards") {

    companion object {
        private var instance: NfcProfileDatabaseHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): NfcProfileDatabaseHelper {
            if (instance == null) {
                instance = NfcProfileDatabaseHelper(ctx.applicationContext)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(NfcCard.TABLE_NAME, true,
                NfcCard.COLUMN_ID to INTEGER + PRIMARY_KEY + UNIQUE,
                NfcCard.COLUMN_NAME to TEXT,
                NfcCard.COLUMN_UID to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

}

val Context.database: NfcProfileDatabaseHelper
    get() {
        return NfcProfileDatabaseHelper.getInstance(applicationContext)
    }