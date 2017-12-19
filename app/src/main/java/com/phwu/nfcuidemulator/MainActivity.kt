package com.phwu.nfcuidemulator

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.*
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.db.*
import org.jetbrains.anko.okButton
import java.io.FileNotFoundException
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

    private val cardList: MutableList<NfcCard> = ArrayList()
    private var cardAdapter: NfcCardArrayAdapter? = null
    private var nfcHelper: NfcHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        nfcHelper = NfcHelper(applicationContext)
        cardAdapter = NfcCardArrayAdapter(
                this,
                R.layout.simple_list_item_2_single_choice,
                cardList
        )

        Snackbar.make(
                findViewById(R.id.layout_main),
                getString(R.string.hint_add_card),
                Snackbar.LENGTH_INDEFINITE
        ).show()

        registerForContextMenu(list_card)
        list_card.adapter = cardAdapter
        list_card.choiceMode = AbsListView.CHOICE_MODE_SINGLE
        list_card.setOnItemClickListener { adapterView, _, position, _ ->
            val adapter = adapterView.adapter
            if (adapter is NfcCardArrayAdapter) {
                switchCard(adapter.getItem(position))
            }
        }

        fab.setOnClickListener { _ ->
            showEditCardDialog({ card ->
                addCard(card)
            })
        }

        loadCards()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_restore_conf -> {
                with(nfcHelper!!) {
                    try {
                        stopService()
                        val result = restoreConfiguration()

                        alert(getString(
                                when (result) {
                                    true -> R.string.title_operation_success
                                    else -> R.string.title_operation_failed
                                }
                        )) {
                            okButton { }
                        }.show()

                        when (result) {
                            true -> cardAdapter!!.setCurrentCard(getString(R.string.card_default_uid))
                            else -> {
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        alert(getString(R.string.msg_orig_conf_not_found)) {
                            title = getString(R.string.title_operation_failed)
                            okButton { }
                        }.show()
                    } finally {
                        startService()
                    }
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (menuInfo is AdapterView.AdapterContextMenuInfo && menu != null) {
            with(menu) {
                add(0, 0, 0, getString(R.string.action_card_edit)).isEnabled = menuInfo.position != 0
                add(0, 0, 0, getString(R.string.action_card_remove)).isEnabled = menuInfo.position != 0
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val menuInfo = item!!.menuInfo
        if (menuInfo is AdapterView.AdapterContextMenuInfo) {
            when (item.title) {
                getString(R.string.action_card_edit) -> {
                    editCard(cardList[menuInfo.position])
                    return true
                }
                getString(R.string.action_card_remove) -> {
                    removeCard(cardList[menuInfo.position])
                    return true
                }
            }
        }

        return false
    }

    override fun onResume() {
        super.onResume()

        val pendingIntent = PendingIntent.getActivity(this,
                0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0
        )

        val filter = IntentFilter()
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val nfcTechList = arrayOf(
                arrayOf(IsoDep::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(NfcB::class.java.name),
                arrayOf(NfcF::class.java.name),
                arrayOf(NfcV::class.java.name),
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name)
        )
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, arrayOf(filter), nfcTechList)
    }

    override fun onPause() {
        super.onPause()

        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent!!.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            val uidBytes = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
            val uid = BigInteger(1, uidBytes).toString(16)
                    .padStart(uidBytes.size * 2, '0')
                    .replace(Regex("..(?!$)"), "$0:")
                    .toUpperCase()

            val detectedCard = NfcCard(NfcCard.ID_EMPTY_CARD, "", uid)
            showEditCardDialog({ card ->
                addCard(card)
            }, detectedCard)
        }
    }

    private fun showEditCardDialog(callback: (NfcCard) -> Unit, card: NfcCard? = null) {
        val fragmentTag = "EditCardFragment"

        val prevFragment = fragmentManager.findFragmentByTag(fragmentTag)
        if (prevFragment == null) {
            val fragment = EditCardFragment()
            val bundle = Bundle()

            bundle.putSerializable("card", card)
            fragment.arguments = bundle
            fragment.setCallback(callback)
            fragment.show(fragmentManager, fragmentTag)
        }
    }

    private fun addCard(card: NfcCard) {
        database.use {
            card.id = insert(NfcCard.TABLE_NAME,
                    NfcCard.COLUMN_NAME to card.name,
                    NfcCard.COLUMN_UID to card.uid)

            cardList.add(card)
        }
    }

    private fun removeCard(card: NfcCard) {
        database.use {
            delete(NfcCard.TABLE_NAME,
                    "${NfcCard.COLUMN_ID} = {${NfcCard.COLUMN_ID}}",
                    NfcCard.COLUMN_ID to card.id)

            cardList.remove(card)
            cardAdapter!!.notifyDataSetChanged()
        }
    }

    private fun editCard(card: NfcCard) {
        showEditCardDialog({ newCard ->
            database.use {
                update(
                        NfcCard.TABLE_NAME,
                        NfcCard.COLUMN_NAME to newCard.name,
                        NfcCard.COLUMN_UID to newCard.uid
                ).whereArgs(
                        "${NfcCard.COLUMN_ID} = {${NfcCard.COLUMN_ID}}",
                        NfcCard.COLUMN_ID to newCard.id
                ).exec()

                cardList.set(cardList.indexOf(card), newCard)
            }
        }, card)
    }

    private fun switchCard(card: NfcCard) {
        var result = false

        with(nfcHelper!!) {
            stopService()
            result = setUid(
                    if (card.id == NfcCard.ID_DEFAULT_CARD) "" else card.uid
            )
            startService()
        }

        if (result) {
            cardAdapter!!.setCurrentCard(card.uid)
        } else {
            alert(getString(R.string.msg_modify_conf_failed)) {
                title = getString(R.string.title_operation_failed)
                okButton { }
            }.show()
        }
    }

    private fun loadCards() {
        val cards = database.use {
            select(NfcCard.TABLE_NAME).exec {
                parseList(classParser<NfcCard>())
            }
        }
        val currentUid = if (cards.none { card ->
            card.uid == nfcHelper!!.getUid()!!
        }) getString(R.string.card_default_uid) else nfcHelper!!.getUid()!!

        cardList.add(NfcCard(
                NfcCard.ID_DEFAULT_CARD,
                getString(R.string.card_default),
                getString(R.string.card_default_uid)
        ))
        cardList.addAll(cards)
        cardAdapter!!.setCurrentCard(currentUid)
    }

}
