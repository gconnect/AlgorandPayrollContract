package com.africinnovate.algorandpayrollsmartcontract

import EmployeeAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.africinnovate.algorandpayrollsmartcontract.Constants.ACCOUNT1_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.ACCOUNT2_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.ACCOUNT3_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.ACCOUNT_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_ADDR
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_TOKEN
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_TOKEN_KEY
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_PORT
import com.africinnovate.algorandpayrollsmartcontract.databinding.ActivityMainBinding
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.algod.client.ApiException
import com.algorand.algosdk.crypto.Address
import com.algorand.algosdk.crypto.Digest
import com.algorand.algosdk.crypto.LogicsigSignature
import com.algorand.algosdk.transaction.SignedTransaction
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.transaction.TxGroup
import com.algorand.algosdk.util.Encoder
import com.algorand.algosdk.v2.client.common.AlgodClient
import com.algorand.algosdk.v2.client.common.Response
import com.algorand.algosdk.v2.client.model.*
import kotlinx.coroutines.*
import org.apache.commons.lang3.ArrayUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.security.Security
import java.util.*
import kotlin.Array as Array1


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    var headers = arrayOf("X-API-Key")
    var values = arrayOf(ALGOD_API_TOKEN_KEY)

    val txHeaders: Array1<String> = ArrayUtils.add(headers, "Content-Type")
    val txValues: Array1<String> = ArrayUtils.add(values, "application/x-binary")
    lateinit var employeeAdapter: EmployeeAdapter
    lateinit var binding: ActivityMainBinding

    private var client: AlgodClient = AlgodClient(
        ALGOD_API_ADDR,
        ALGOD_PORT,
        ALGOD_API_TOKEN,
        ALGOD_API_TOKEN_KEY
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)

        val address = Address(Constants.ACCOUNT_ADDRESS)
        getAccountBalance(address)

        initializeRecyclerview()

        employeeAdapter.onItemClick = { position ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("employee", position)
            startActivity(intent)
        }

        binding.copy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", Constants.ACCOUNT_ADDRESS)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, Constants.ACCOUNT_ADDRESS, Toast.LENGTH_LONG).show()
        }
        binding.fund.setOnClickListener { fundAccount() }
        binding.explore.setOnClickListener { viewExlorer() }

        binding.swipe.setOnRefreshListener {
            getAccountBalance(address)
            binding.swipe.isRefreshing = false
        }
        //Use this to generate accounts
        val account = Account()
        Timber.d("mnemonic1 ${account.toMnemonic()} ")
        Timber.d("address1 ${account.address} ")

        binding.paybtn.setOnClickListener {
//            atomicTransferWithSmartContract()
            atomicTransfer()
        }
        binding.root

    }

    private fun getAccountBalance(address: Address?) = launch {
        runOnUiThread {
            binding.progress1.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            try {
                val accountInfo = client.AccountInformation(address).execute(headers, values).body()
                Timber.d("Account Balance: ${accountInfo.amount}")
                runOnUiThread {
                    binding.progress1.visibility = View.GONE
                    val amount = accountInfo.amount.toBigDecimal()
                    binding.accountBal.text = amount.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    private fun viewExlorer() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.EXLORER))
        try {
            if (!Constants.EXLORER.startsWith("http://") && !Constants.EXLORER.startsWith(
                    "https://"
                )
            )
                Constants.EXLORER = "http://" + Constants.EXLORER;
            startActivity(browserIntent)
        } catch (e: Exception) {
            Timber.d("Host not available ${e.message}")
        }
    }

    private fun fundAccount() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FUND_ACCOUNT))
        try {
            if (!Constants.FUND_ACCOUNT.startsWith("http://") && !Constants.FUND_ACCOUNT.startsWith(
                    "https://"
                )
            )
                Constants.FUND_ACCOUNT = "http://" + Constants.FUND_ACCOUNT;
            startActivity(browserIntent)
        } catch (e: Exception) {
            Timber.d("Host not available ${e.message}")
        }
    }


    private fun initializeRecyclerview() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        employeeAdapter = EmployeeAdapter(this)
        recyclerView.adapter = employeeAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        employeeAdapter.setData(EmployeeDataSource.createEmployeeDate())
    }

    // utility function to connect to a node
    private fun connectToNetwork(): AlgodClient {
        return client
    }

    private fun waitForConfirmation(txID: String) {
        if (client == null) client = connectToNetwork()
        var lastRound = client!!.GetStatus().execute(headers, values).body().lastRound
        while (true) {
            try {
                //Check the pending tranactions
                val pendingInfo: Response<PendingTransactionResponse> =
                    client.PendingTransactionInformation(txID).execute(headers, values)
                if (pendingInfo.body().confirmedRound != null && pendingInfo.body().confirmedRound > 0) {
                    //Got the completed Transaction
                    println("Transaction " + txID + " confirmed in round " + pendingInfo.body().confirmedRound)
                    runOnUiThread {
                        binding.result.text =
                            "Transaction $txID  confirmed in round ${pendingInfo.body().confirmedRound}"
                    }
                    break
                }
                lastRound++
                client.WaitForBlock(lastRound).execute(headers, values)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    //Atomic Transfer signed by the Sender
    @RequiresApi(Build.VERSION_CODES.O)
    private fun atomicTransfer() = launch {
        runOnUiThread {
            binding.progress1.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            val employer_mnemonic = ACCOUNT_MNEMONIC
            val employee1_mnemonic = ACCOUNT1_MNEMONIC
            val employee2_mnemonic = ACCOUNT2_MNEMONIC
            val employee3_mnemonic = ACCOUNT3_MNEMONIC

            // recover account A, B, C
            val acctA = Account(employer_mnemonic)
            val acctB = Account(employee1_mnemonic)
            val acctC = Account(employee2_mnemonic)
            val acctD = Account(employee3_mnemonic)

            // get node suggested parameters
            val params = client.TransactionParams().execute(headers, values).body()
            // Create the first transaction
            val tx1: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(acctA.address)
                .amount(2000000)
                .receiver(acctB.address)
                .suggestedParams(params)
                .build()

            // Create the second transaction
            val tx2: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(acctA.address)
                .amount(1000000)
                .receiver(acctC.address)
                .suggestedParams(params)
                .build()

            // Create the third transaction
            val tx3: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(acctA.address)
                .amount(2000000)
                .receiver(acctD.address)
                .suggestedParams(params)
                .build()

            // group transactions an assign ids
            val gid: Digest = TxGroup.computeGroupID(*arrayOf(tx1, tx2, tx3))
            tx1.assignGroupID(gid)
            tx2.assignGroupID(gid)
            tx3.assignGroupID(gid)

            // sign individual transactions
            val signedTx1: SignedTransaction = acctA.signTransaction(tx1)
            val signedTx2: SignedTransaction = acctA.signTransaction(tx2)
            val signedTx3: SignedTransaction = acctA.signTransaction(tx3)


            try {
                // put all transactions in a byte array
                val byteOutputStream = ByteArrayOutputStream()
                val encodedTxBytes1: ByteArray = Encoder.encodeToMsgPack(signedTx1)
                val encodedTxBytes2: ByteArray = Encoder.encodeToMsgPack(signedTx2)
                val encodedTxBytes3: ByteArray = Encoder.encodeToMsgPack(signedTx3)
                byteOutputStream.write(encodedTxBytes1)
                byteOutputStream.write(encodedTxBytes2)
                byteOutputStream.write(encodedTxBytes3)
                val groupTransactionBytes: ByteArray = byteOutputStream.toByteArray()

                // send transaction group
                val id = client.RawTransaction().rawtxn(groupTransactionBytes).execute(
                    txHeaders,
                    txValues
                ).body().txId
                println("Successfully sent tx with ID: $id")
                runOnUiThread {
                    binding.progress1.visibility = View.GONE
                    binding.result.text = "Successfully sent tx with ID: $id"
                }
                // wait for confirmation
                waitForConfirmation(id)
            } catch (e: java.lang.Exception) {
                println("Submit Exception: $e")
            }
        }
    }


    // Atomic Transfer Signed with a smart Contract Logic Sig
    @RequiresApi(Build.VERSION_CODES.O)
    private fun atomicTransferWithSmartContract() = launch {
        runOnUiThread {
            binding.progress1.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {

            val source = """
            arg_0
            btoi
            int 123
            ==
        """.trimIndent()

            // compile
            val response =
                client.TealCompile().source(source.toByteArray(charset("UTF-8"))).execute(
                    headers,
                    values
                ).body()
            // print results
            println("response: $response")
            println("Hash: " + response.hash)
            println("Result: " + response.result)
            val program = Base64.getDecoder().decode(response.result.toString())

            // create logic sig
            // string parameter
//            val teal_args = ArrayList<ByteArray>()
//             val orig = "Fee"
//             teal_args.add(orig.toByteArray())
            // LogicsigSignature lsig = new LogicsigSignature(program, teal_args);

            // integer parameter
            val teal_args = ArrayList<ByteArray>()
            val arg1 = byteArrayOf(123)
            teal_args.add(arg1)
            val lsig = LogicsigSignature(program, teal_args)
            Timber.d("lsig add ${lsig.toAddress()}")

            val employer_mnemonic = ACCOUNT_MNEMONIC
            val employee1_mnemonic = ACCOUNT1_MNEMONIC
            val employee2_mnemonic = ACCOUNT2_MNEMONIC
            val employee3_mnemonic = ACCOUNT3_MNEMONIC

            // recover account A, B, C
            val acctA = Account(employer_mnemonic)
            val acctB = Account(employee1_mnemonic)
            val acctC = Account(employee2_mnemonic)
            val acctD = Account(employee3_mnemonic)

            // get node suggested parameters
            val params = client.TransactionParams().execute(headers, values).body()
            // Create the first transaction
            val tx1: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(lsig.toAddress())
                .amount(2000000)
                .receiver(acctB.address)
                .suggestedParams(params)
                .build()

            // Create the second transaction
            val tx2: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(lsig.toAddress())
                .amount(1000000)
                .receiver(acctC.address)
                .suggestedParams(params)
                .build()

            // Create the third transaction
            val tx3: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(lsig.toAddress())
                .amount(2000000)
                .receiver(acctD.address)
                .suggestedParams(params)
                .build()

            // group transactions an assign ids
            val gid: Digest = TxGroup.computeGroupID(*arrayOf(tx1, tx2, tx3))
            tx1.assignGroupID(gid)
            tx2.assignGroupID(gid)
            tx3.assignGroupID(gid)


            // create the LogicSigTransaction with contract account LogicSig
            // sign individual transactions
            val signedTx1: SignedTransaction = Account.signLogicsigTransaction(lsig, tx1)
            val signedTx2: SignedTransaction = Account.signLogicsigTransaction(lsig, tx2)
            val signedTx3: SignedTransaction = Account.signLogicsigTransaction(lsig, tx3)
            try {
                // put all transactions in a byte array
                val byteOutputStream = ByteArrayOutputStream()
                val encodedTxBytes1: ByteArray = Encoder.encodeToMsgPack(signedTx1)
                val encodedTxBytes2: ByteArray = Encoder.encodeToMsgPack(signedTx2)
                val encodedTxBytes3: ByteArray = Encoder.encodeToMsgPack(signedTx3)
                byteOutputStream.write(encodedTxBytes1)
                byteOutputStream.write(encodedTxBytes2)
                byteOutputStream.write(encodedTxBytes3)
                val groupTransactionBytes: ByteArray = byteOutputStream.toByteArray()

                // send transaction group
                val id = client.RawTransaction().rawtxn(groupTransactionBytes).execute(
                    txHeaders,
                    txValues
                ).body().txId
                println("Successfully sent tx with ID: $id")
                runOnUiThread {
                    binding.progress1.visibility = View.GONE
                    binding.result.text = "Successfully sent tx with ID: $id"
                }
                // wait for confirmation
                waitForConfirmation(id)
            } catch (e: java.lang.Exception) {
                println("Submit Exception: $e")
            }
        }
    }

}