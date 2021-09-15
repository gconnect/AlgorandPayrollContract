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
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_ADDR
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_TOKEN
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_API_TOKEN_KEY
import com.africinnovate.algorandpayrollsmartcontract.Constants.ALGOD_PORT
import com.africinnovate.algorandpayrollsmartcontract.Constants.EMPLOYEE1_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.EMPLOYEE2_MNEMONIC
import com.africinnovate.algorandpayrollsmartcontract.Constants.EMPLOYER_MNEMONIC
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


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    var headers = arrayOf("X-API-Key")
    var values = arrayOf(ALGOD_API_TOKEN_KEY)

    val txHeaders: Array<String> = ArrayUtils.add(headers, "Content-Type")
    val txValues: Array<String> = ArrayUtils.add(values, "application/x-binary")
    lateinit var employeeAdapter: EmployeeAdapter
    var selectedEmployee: Boolean = false
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
        val address = Address(Constants.EMPLOYER_ADDRESS)
        getAccountBalance(address)


        initializeRecyclerview()

        binding.paybtn.setOnClickListener {
            contractAccount()
        }

        employeeAdapter.onItemClick = { position ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("employee", position)
            startActivity(intent)
        }

        employeeAdapter.onItemChecked = { checkValue, position ->
            selectedEmployee = checkValue
            Timber.d("check $checkValue at position $position")
            Timber.d("selectedEmployee $checkValue at position $position")
        }

        binding.copy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", Constants.EMPLOYER_ADDRESS)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, Constants.EMPLOYER_ADDRESS, Toast.LENGTH_LONG).show()
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

        binding.root

    }

    private fun getAccountBalance(address: Address?) = launch {
        runOnUiThread{
            binding.progress1.visibility = View.VISIBLE
        }
        withContext(Dispatchers.Default) {
            val accountInfo = client.AccountInformation(address).execute(headers, values).body()
            Timber.d("Account Balance: ${accountInfo.amount}")
            runOnUiThread {
                binding.progress1.visibility = View.GONE
                binding.accountBal.text = accountInfo.amount.toString()
            }
        }
//            return accountInfo.amount
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
                        "Transaction $txID  confirmed in round ${pendingInfo.body().confirmedRound}" }

                    break
                }
                lastRound++
                client.WaitForBlock(lastRound).execute(headers, values)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // Atomic Transfer

    private fun atomicTransfer() {
        if (client == null) client = connectToNetwork()
        val employer_mnemonic = EMPLOYER_MNEMONIC
        val employee1_mnemonic = EMPLOYEE1_MNEMONIC
        val employee2_mnemonic = EMPLOYEE2_MNEMONIC

        // recover account A, B, C
        val acctA = Account(employer_mnemonic)
        val acctB = Account(employee1_mnemonic)
        val acctC = Account(employee2_mnemonic)

        // get node suggested parameters
        val params = client.TransactionParams().execute(headers, values).body()

        // Create the first transaction
        val tx1: Transaction = Transaction.PaymentTransactionBuilder()
            .sender(acctA.address)
            .amount(10000)
            .receiver(acctB.address)
            .suggestedParams(params)
            .build()

        // Create the second transaction
        val tx2: Transaction = Transaction.PaymentTransactionBuilder()
            .sender(acctA.address)
            .amount(20000)
            .receiver(acctC.address)
            .suggestedParams(params)
            .build()
        // group transactions an assign ids
        val gid: Digest = TxGroup.computeGroupID(*arrayOf(tx1, tx2))
        tx1.assignGroupID(gid)
        tx2.assignGroupID(gid)

        // sign individual transactions
        val signedTx1: SignedTransaction = acctA.signTransaction(tx1)
        val signedTx2: SignedTransaction = acctA.signTransaction(tx2)
        try {
            // put both transaction in a byte array
            val byteOutputStream = ByteArrayOutputStream()
            val encodedTxBytes1: ByteArray = Encoder.encodeToMsgPack(signedTx1)
            val encodedTxBytes2: ByteArray = Encoder.encodeToMsgPack(signedTx2)
            byteOutputStream.write(encodedTxBytes1)
            byteOutputStream.write(encodedTxBytes2)
            val groupTransactionBytes: ByteArray = byteOutputStream.toByteArray()
            print(groupTransactionBytes)

            // send transaction group
            val id = client.RawTransaction().rawtxn(groupTransactionBytes).execute(
                txHeaders,
                txValues
            ).body().txId
            println("Successfully sent tx with ID: $id")

            binding.result.text = "Successfully sent tx with ID: $id"
            // wait for confirmation
            waitForConfirmation(id)
        } catch (e: java.lang.Exception) {
            println("Submit Exception: $e")
        }
    }

    //Stateless SmartContract
    fun compileTealSource() {
        // Initialize an algod client
        if (client == null) client = connectToNetwork()

        // read file - int 0
//      val data: ByteArray = Files.readAllBytes(Paths.get("/sample.teal"))
//        val data = byteArrayOf(0)
        val data1 = "int 1"

//        Timber.d("data : ${data.contentToString()}")

        val response: CompileResponse =
            client.TealCompile().source(data1.toByteArray(charset("UTF-8"))).execute(
                headers,
                values
            ).body()
        // print results
        Timber.d("response: $response")
        Timber.d("Hash: " + response.hash)
        Timber.d("Result: " + response.result)
        binding.result.text =
            "response: $response\n Hash: ${response.hash}\n Result: \" + ${response.result}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun contractAccount() = launch {

        runOnUiThread{
            binding.progress.visibility = View.VISIBLE
        }

        withContext(Dispatchers.Default){
            // Initialize an algod client
            if (client == null) client = connectToNetwork()

            // Set the receiver
            val RECEIVER = "QUDVUXBX4Q3Y2H5K2AG3QWEOMY374WO62YNJFFGUTMOJ7FB74CMBKY6LPQ"

            // Read program from file samplearg.teal
//        val source = readAllBytes(Paths.get("./samplearg.teal"))
            val source = """
            arg_0
            btoi
            int 123
            ==
        """.trimIndent()
            // compile
            val response = client.TealCompile().source(source.toByteArray(charset("UTF-8"))).execute(
                headers,
                values
            ).body()
            // print results
            println("response: $response")
            println("Hash: " + response.hash)
            println("Result: " + response.result)
            val program = Base64.getDecoder().decode(response.result.toString())

            // create logic sig
            // integer parameter
            val teal_args = ArrayList<ByteArray>()
            val arg1 = byteArrayOf(123)
            teal_args.add(arg1)
            val lsig = LogicsigSignature(program, teal_args)
            // For no args use null as second param
            // LogicsigSignature lsig = new LogicsigSignature(program, null);
            println("lsig address: " + lsig.toAddress())
            val params = client.TransactionParams().execute(headers, values).body()
            // create a transaction
            val note = "Hello World"
            val txn: Transaction = Transaction.PaymentTransactionBuilder()
                .sender(
                    lsig
                        .toAddress()
                )
                .note(note.toByteArray())
                .amount(100000)
                .receiver(Address(RECEIVER))
                .suggestedParams(params)
                .build()
            try {
                // create the LogicSigTransaction with contract account LogicSig
                val stx: SignedTransaction = Account.signLogicsigTransaction(lsig, txn)
                // send raw LogicSigTransaction to network
                val encodedTxBytes: ByteArray = Encoder.encodeToMsgPack(stx)
                val id = client.RawTransaction().rawtxn(encodedTxBytes).execute(txHeaders, txValues)
                    .body().txId
                // Wait for transaction confirmation
                waitForConfirmation(id)
                //Dryrun Debugging
//            getDryrunResponse(stx, source.toByteArray(charset("UTF-8")))

                println("Successfully sent tx with id: $id")
                // Read the transaction
                val pTrx = client.PendingTransactionInformation(id).execute(headers, values).body()
                val jsonObj = JSONObject(pTrx.toString())
                println("Transaction information (with notes): " + jsonObj.toString(2)) // pretty print
                println("Decoded note: " + String(pTrx.txn.tx.note))

                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    binding.result.text =
                        "Successfully sent tx with id: $id\n Transaction information (with notes): ${jsonObj.toString(2)}\n  " + "Decoded note: ${String(pTrx.txn.tx.note)}"
                }

            } catch (e: ApiException) {
                System.err.println("Exception when calling algod#rawTransaction: " + e.getResponseBody())
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun accountDelegation() {
        // Initialize an algod client
        if (client == null) client = connectToNetwork()
        // import your private key mnemonic and address
        val SRC_ACCOUNT =
            "buzz genre work meat fame favorite rookie stay tennis demand panic busy hedgehog snow morning acquire ball grain grape member blur armor foil ability seminar"

        val src = Account(SRC_ACCOUNT)
        // Set the receiver
        val RECEIVER = "QUDVUXBX4Q3Y2H5K2AG3QWEOMY374WO62YNJFFGUTMOJ7FB74CMBKY6LPQ"

        // Read program from file samplearg.teal
//        val source = readAllBytes(Paths.get("./samplearg.teal"))
        val source = """
            arg_0
            btoi
            int 123
            ==
        """.trimIndent()
        // compile
        val response = client.TealCompile().source(source.toByteArray(charset("UTF-8")))
            .execute(headers, values).body()
        // print results
        println("response: $response")
        println("Hash: " + response.hash)
        println("Result: " + response.result)
        val program = Base64.getDecoder().decode(response.result.toString())

        // create logic sig

        // string parameter
        // ArrayList<byte[]> teal_args = new ArrayList<byte[]>();
        // String orig = "my string";
        // teal_args.add(orig.getBytes());
        // LogicsigSignature lsig = new LogicsigSignature(program, teal_args);

        // integer parameter
        val teal_args = ArrayList<ByteArray>()
        val arg1 = byteArrayOf(123)
        teal_args.add(arg1)
        val lsig = LogicsigSignature(program, teal_args)
        //    For no args use null as second param
        //    LogicsigSignature lsig = new LogicsigSignature(program, null);
        // sign the logic signature with an account sk
        src.signLogicsig(lsig)
        val params = client.TransactionParams().execute(headers, values).body()
        // create a transaction
        val note = "Hello World"
        val txn = Transaction.PaymentTransactionBuilder()
            .sender(src.address)
            .note(note.toByteArray())
            .amount(100000)
            .receiver(Address(RECEIVER))
            .suggestedParams(params)
            .build()
        try {
            // create the LogicSigTransaction with contract account LogicSig
            val stx = Account.signLogicsigTransaction(lsig, txn)
            // send raw LogicSigTransaction to network
            val encodedTxBytes = Encoder.encodeToMsgPack(stx)
            val id = client.RawTransaction().rawtxn(encodedTxBytes).execute(txHeaders, txValues)
                .body().txId
            // Wait for transaction confirmation
            waitForConfirmation(id)
            println("Successfully sent tx with id: $id")
            // Read the transaction
            val pTrx = client.PendingTransactionInformation(id).execute(headers, values).body()
            val jsonObj = JSONObject(pTrx.toString())
            println("Transaction information (with notes): " + jsonObj.toString(2)) // pretty print
            println("Decoded note: " + String(pTrx.txn.tx.note))
        } catch (e: ApiException) {
            System.err.println("Exception when calling algod#rawTransaction: " + e.responseBody)
        }
    }

    private fun getDryrunResponse(
        stxn: SignedTransaction,
        source: ByteArray?
    ): Response<DryrunResponse?>? {
        val sources: MutableList<DryrunSource> = ArrayList()
        val stxns: MutableList<SignedTransaction> = ArrayList()
        // compiled
        if (source == null) {
            stxns.add(stxn)
        } else if (source != null) {
            val drs = DryrunSource()
            drs.fieldName = "lsig"
            drs.source = String(source)
            drs.txnIndex = 0L
            sources.add(drs)
            stxns.add(stxn)
        }
        val dryrunResponse: Response<DryrunResponse?>
        val dr = DryrunRequest()
        dr.txns = stxns
        dr.sources = sources
        dryrunResponse = client.TealDryrun().request(dr).execute()
        return dryrunResponse
    }

}