package com.africinnovate.algorandpayrollsmartcontract

object Constants {
    const val HEADER_VALUE = "x-api-key: YOUR PURESTAKE API-KEY"
    var FUND_ACCOUNT = "https://testnet.algoexplorer.io/dispenser"
    var EXLORER = "https://testnet.algoexplorer.io/"
    const val ALGOD_API_ADDR = "https://testnet-algorand.api.purestake.io/ps2/"
    const val ALGOD_PORT = 443

    const val ALGOD_API_TOKEN_KEY = "YOUR PURESTAKE API KEY"
    const val ALGOD_API_TOKEN = "B3SU4KcVKi94Jap2VXkK83xx38bsv95K5UZm2lab"

    //New Accounts
    const val ACCOUNT1_MNEMONIC = "create yours"
    const val ACCOUNT1_ADDRESS = "create yours"

    const val ACCOUNT2_MNEMONIC = "create yours"
    const val ACCOUNT2_ADDRESS = "create yoursI"

    const val ACCOUNT3_MNEMONIC = "create yours"
    const val ACCOUNT3_ADDRESS = "create yours"

    const val ACCOUNT_MNEMONIC = "create yours"
    const val ACCOUNT_ADDRESS = "create yours"



    // Teal program
    val tealSource = """
                int 1
                
                // Check the Fee is resonable, In this case 10,000 microalgos
                txn Fee
                int 10000
                <=
                &&
                
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
              
            """.trimIndent()
}

