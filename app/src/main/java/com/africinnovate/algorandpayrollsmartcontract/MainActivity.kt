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
import com.algorand.algosdk.crypto.Address
import com.algorand.algosdk.crypto.Digest
import com.algorand.algosdk.crypto.LogicsigSignature
import com.algorand.algosdk.transaction.SignedTransaction
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.transaction.TxGroup
import com.algorand.algosdk.util.Encoder
import com.algorand.algosdk.v2.client.common.AlgodClient
import com.algorand.algosdk.v2.client.common.Response
import com.algorand.algosdk.v2.client.model.CompileResponse
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse
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
    val source = Constants.tealSource
    lateinit var response: Response<CompileResponse>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)

        val address = Address(Constants.LSIG_SENDER_ADDRESS)
        getAccountBalance(address)

        initializeRecyclerview()

        employeeAdapter.onItemClick = { position ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("employee", position)
            startActivity(intent)
        }

        binding.copy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", Constants.LSIG_SENDER_ADDRESS)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, Constants.LSIG_SENDER_ADDRESS, Toast.LENGTH_LONG).show()
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
            atomicTransferWithSmartContract()
//            atomicTransfer()
        }

        binding.compile.setOnClickListener {
            compileTeal()
        }
        binding.root

    }

    private fun getAccountBalance(address: Address?) = launch {
        runOnUiThread {
            binding.progress1.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            try {
                val respAcct = client.AccountInformation(address).execute(headers, values)
                if (!respAcct.isSuccessful) {
                    throw java.lang.Exception(respAcct.message())
                }
                val accountInfo = respAcct.body()
                println(String.format("Account Balance: %d microAlgos", accountInfo.amount))
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

    /**
     * utility function to wait on a transaction to be confirmed
     * the timeout parameter indicates how many rounds do you wish to check pending transactions for
     */
    private fun waitForConfirmation(
        myclient: AlgodClient?,
        txID: String?,
        timeout: Int
    ): PendingTransactionResponse? {
        require(!(myclient == null || txID == null || timeout < 0)) { "Bad arguments for waitForConfirmation." }
        var resp = myclient.GetStatus().execute(headers, values)
        if (!resp.isSuccessful) {
            throw java.lang.Exception(resp.message())
        }
        val nodeStatusResponse = resp.body()
        val startRound = nodeStatusResponse.lastRound + 1
        var currentRound = startRound
        while (currentRound < startRound + timeout) {
            // Check the pending transactions
            val resp2 = myclient.PendingTransactionInformation(txID).execute(headers, values)
            if (resp2.isSuccessful) {
                val pendingInfo = resp2.body()
                if (pendingInfo != null) {
                    if (pendingInfo.confirmedRound != null && pendingInfo.confirmedRound > 0) {
                        // Got the completed Transaction
                        println("Transaction " + txID + " confirmed in round " + pendingInfo.confirmedRound)
                        runOnUiThread {
                            binding.result.text =
                                "Transaction $txID  confirmed in round ${pendingInfo.confirmedRound}"
                        }
                        return pendingInfo
                    }
                    if (pendingInfo.poolError != null && pendingInfo.poolError.length > 0) {
                        // If there was a pool error, then the transaction has been rejected!
                        throw java.lang.Exception("The transaction has been rejected with a pool error: " + pendingInfo.poolError)
                    }
                }
            }
            resp = myclient.WaitForBlock(currentRound).execute(headers, values)
            if (!resp.isSuccessful) {
                throw java.lang.Exception(resp.message())
            }
            currentRound++
        }
        throw java.lang.Exception("Transaction not confirmed after $timeout rounds!")
    }


    //Atomic Transfer signed by the Sender
    @RequiresApi(Build.VERSION_CODES.O)
    private fun atomicTransfer() = launch {
        runOnUiThread {
            binding.progress.visibility = View.VISIBLE
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

            var resp = client.TransactionParams().execute(headers, values)
            if (!resp.isSuccessful) {
                throw java.lang.Exception(resp.message())
            }
            val params = resp.body() ?: throw  Exception("Params retrieval error")
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

                // send transaction group to the network
                val rawResponse = client.RawTransaction().rawtxn(groupTransactionBytes).execute(
                    txHeaders,
                    txValues
                )
                if (!rawResponse.isSuccessful()) {
                    throw  Exception(rawResponse.message());
                }
                val id = rawResponse.body().txId

                println("Successfully sent tx with ID: $id")
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    binding.result.text = "Successfully sent tx with ID: $id"
                }

                // Wait for transaction confirmation
                val pTrx = waitForConfirmation(client, id, 4)
                System.out.println("Transaction " + id.toString() + " confirmed in round " + pTrx!!.confirmedRound)
                // Read the transaction
                val jsonObj2 = JSONObject(pTrx.toString())
                println("Transaction information (with notes): " + jsonObj2.toString(2))
                println("Decoded note: " + String(pTrx.txn.tx.note))
                println("Transaction information (with notes): " + jsonObj2.toString(2))
                println("Decoded note: " + String(pTrx.txn.tx.note))
                println("Amount: " + pTrx.txn.tx.amount.toString())
                println("Fee: " + pTrx.txn.tx.fee.toString())
                if (pTrx.closingAmount != null) {
                    println("Closing Amount: " + pTrx.closingAmount.toString())
                }
            } catch (e: java.lang.Exception) {
                println("Submit Exception: $e")
                System.err.println("Exception when calling algod#transactionInformation: " + e.message);

            }
        }
    }

    private fun compileTeal() = launch {
        runOnUiThread {
            binding.progress.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            try {
                response = client.TealCompile().source(source.toByteArray(charset("UTF-8"))).execute(
                    headers,
                    values
                )
                if (!response.isSuccessful) {
                    throw java.lang.Exception(response.message().toString())
                }
                Timber.d("compileResponse: hash ${response.body().hash}")
                Timber.d("compileResponse: result ${response.body().result}")
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    binding.result.text =
                        " Response:\n Hash: ${response.body().hash}\n Result:  ${response.body().result}"
                }
                            
            } catch (e: Throwable) {
                e.printStackTrace()
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    binding.result.text =
                        "Error occured, check the TEAL program"
                }
            }

        }
    }

    // Atomic Transfer Signed with a smart Contract Logic Sig
    @RequiresApi(Build.VERSION_CODES.O)
    private fun atomicTransferWithSmartContract() = launch {
        runOnUiThread {
            binding.progress.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            try {
                val program = Base64.getDecoder().decode(response.body().result.toString())

                val lsig = LogicsigSignature(program, null)
                Timber.d("lsig add ${lsig.toAddress()}")

                val employee1_mnemonic = ACCOUNT1_MNEMONIC
                val employee2_mnemonic = ACCOUNT2_MNEMONIC
                val employee3_mnemonic = ACCOUNT3_MNEMONIC

                // recover account A, B, C
                val acctA = Account(employee1_mnemonic)
                val acctB = Account(employee2_mnemonic)
                val acctC = Account(employee3_mnemonic)

                // get node suggested parameters
                var resp = client.TransactionParams().execute(headers, values)
                if (!resp.isSuccessful) {
                    throw java.lang.Exception(resp.message())
                }
                val params = resp.body() ?: throw  Exception("Params retrieval error")
                // Create the first transaction
                val tx1: Transaction = Transaction.PaymentTransactionBuilder()
                    .sender(lsig.toAddress())
                    .amount(2000000)
                    .receiver(acctA.address)
                    .suggestedParams(params)
                    .note("employee1".toByteArray())
                    .build()

                // Create the second transaction
                val tx2: Transaction = Transaction.PaymentTransactionBuilder()
                    .sender(lsig.toAddress())
                    .amount(1000000)
                    .receiver(acctB.address)
                    .suggestedParams(params)
                    .note("employee2".toByteArray())
                    .build()

                // Create the third transaction
                val tx3: Transaction = Transaction.PaymentTransactionBuilder()
                    .sender(lsig.toAddress())
                    .amount(2000000)
                    .receiver(acctC.address)
                    .suggestedParams(params)
                    .note("employee3".toByteArray())
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

                // put all transactions in a byte array
                val byteOutputStream = ByteArrayOutputStream()
                val encodedTxBytes1: ByteArray = Encoder.encodeToMsgPack(signedTx1)
                val encodedTxBytes2: ByteArray = Encoder.encodeToMsgPack(signedTx2)
                val encodedTxBytes3: ByteArray = Encoder.encodeToMsgPack(signedTx3)
                byteOutputStream.write(encodedTxBytes1)
                byteOutputStream.write(encodedTxBytes2)
                byteOutputStream.write(encodedTxBytes3)
                val groupTransactionBytes: ByteArray = byteOutputStream.toByteArray()

                // Submit transaction group to the network
                val rawResponse = client.RawTransaction().rawtxn(groupTransactionBytes).execute(
                    txHeaders,
                    txValues
                )
                if (!rawResponse.isSuccessful()) {
                    throw  Exception(rawResponse.message());
                }
                val id = rawResponse.body().txId

                println("Successfully sent tx with ID: $id")
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    binding.result.text = "Successfully sent tx with ID: $id"
                }

                // Wait for transaction confirmation
                val pTrx = waitForConfirmation(client, id, 4)
                System.out.println("Transaction " + id.toString() + " confirmed in round " + pTrx!!.confirmedRound)
                // Read the transaction
                val jsonObj2 = JSONObject(pTrx.toString())
                println("Transaction information (with notes): " + jsonObj2.toString(2))
                println("Decoded note: " + String(pTrx.txn.tx.note))
                println("Transaction information (with notes): " + jsonObj2.toString(2))
                println("Decoded note: " + String(pTrx.txn.tx.note))
                println("Amount: " + pTrx.txn.tx.amount.toString())
                println("Fee: " + pTrx.txn.tx.fee.toString())
                if (pTrx.closingAmount != null) {
                    println("Closing Amount: " + pTrx.closingAmount.toString())
                }
            } catch (e: java.lang.Exception) {
                System.err.println("Exception when calling algod#transactionInformation: " + e.message);
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        binding.result.text =
                            "${e.message}"
                    }
            }
        }
    }
}