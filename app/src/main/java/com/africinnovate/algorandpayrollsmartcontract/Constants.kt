                                       package com.africinnovate.algorandpayrollsmartcontract

object Constants {
    const val HEADER_VALUE = "x-api-key: YOUR PURESTAKE API-KEY"
    var FUND_ACCOUNT = "https://dispenser.testnet.aws.algodev.network/"
    var EXLORER = "https://testnet.algoexplorer.io/"
    const val ALGOD_API_ADDR = "https://testnet-algorand.api.purestake.io/ps2/"
    const val ALGOD_PORT = 443

    const val ALGOD_API_TOKEN_KEY = "YOUR PURESTAKE API KEY"
    const val ALGOD_API_TOKEN = "B3SU4KcVKi94Jap2VXkK83xx38bsv95K5UZm2lab"

    //New Accounts
//    const val ACCOUNT1_MNEMONIC = "create yours"
//    const val ACCOUNT1_ADDRESS = "create yours"
//
//    const val ACCOUNT2_MNEMONIC = "create yours"
//    const val ACCOUNT2_ADDRESS = "create yoursI"
//
//    const val ACCOUNT3_MNEMONIC = "create yours"
//    const val ACCOUNT3_ADDRESS = "create yours"
//
//    const val ACCOUNT_MNEMONIC = "create yours"
//    const val ACCOUNT_ADDRESS = "create yours"



    // Teal Generated Algorand Address
    const val LSIG_SENDER_ADDRESS = "2PIJ4SHZNJ4YJGCNVQSN6UEWSNVI7WOHWOONNE3U4G2P3CTYHQYUCLQXWY"

    // Teal program
    val tealSource = """  
               // Check the Fee is resonable, In this case 10,000 microalgos
                txn Fee
                int 10000
                <=
                //Check that the first group transaction is equal to 2000000
                gtxn 0 Amount
                int 2000000
                ==
                &&

                //Check that the second group transaction is equal to 1000000
                gtxn 1 Amount
                int 1000000
                == // same here, need to add an evaluation
                &&

                //Check that the third group transaction is equal to 2000000
                gtxn 2 Amount
                int 2000000
                ==
                &&

                //Check that the transaction amount is less than 5000000 or equal to 5000000
                txn Amount
                int 5000000
                <=
                &&
                //CloseRemainderTo should be the intended recipient or equal to global ZeroAddress.
                 // CloseRemainder can be used to reset sender account to 0.
                 // WARNING! all remaining funds in the sender
//                account will be sent to the closeRemainderTo Account, omit RECEIVER account when
//                in use otherwise all funds from the sender account will be sent to that account.
               
                txn CloseRemainderTo 
                global ZeroAddress
                ==
                &&
                //Always verify that the RekeyTo property of any transaction is set to the ZeroAddress
                //unless the contract is specifically involved ina rekeying operation.
                //Rekeying is a powerful protocol feature which enables an Algorand account holder to
                //maintain a static public address while dynamically rotating the authoritative private
                //spending key(s).
                //This check to prevent the transaction from been assigned to a new private key.
//                txn RekeyTo
//                global ZeroAddress
//                ==
//                &&
//                && 
            """.trimIndent()

}

