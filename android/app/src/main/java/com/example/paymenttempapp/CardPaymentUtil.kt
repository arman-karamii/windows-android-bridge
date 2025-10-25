package com.example.paymenttempapp

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

const val TAG = "CardPaymentUtil"

fun behTransactionDtoConverter(resultBeh: String): TransactionResult? {
    var terminalId = ""
    var responseCode = ""
    var description = ""
    var stan = ""
    var rrn = ""
    var pan = ""
    var transactionDate = ""
    var transactionTime = ""
    var status = 0 // AUTH status
    var amount = "0"
    var merchantId = ""
    
    try {
        try {
            val jsonObject = JSONObject(resultBeh)
            if (jsonObject.getString("resultCode").equals("000")) {
                terminalId = jsonObject.getString("terminalID")
                status = 1 // SETTLED status
                if (jsonObject.has("retrievalReferencedNumber")) {
                    stan = jsonObject.getString("retrievalReferencedNumber")
                }
                if (jsonObject.has("referenceID")) {
                    rrn = jsonObject.getString("referenceID")
                }
                responseCode = jsonObject.getString("resultCode")
                pan = jsonObject.getString("maskedCardNumber")
                transactionDate = jsonObject.getString("dateOfTransaction")
                transactionTime = jsonObject.getString("timeOfTransaction")
                amount = if (jsonObject.has("transactionAmount")) jsonObject.getString("transactionAmount") else "0"
            } else {
                status = 2 // SETTLE_FAIL status
                if (jsonObject.has("retrievalReferencedNumber")) {
                    stan = jsonObject.getString("retrievalReferencedNumber")
                }
                if (jsonObject.has("referenceID")) {
                    rrn = jsonObject.getString("referenceID")
                }
                if (jsonObject.has("terminalID")) {
                    terminalId = jsonObject.getString("terminalID")
                }
                if (jsonObject.has("resultCode")) {
                    responseCode = jsonObject.getString("resultCode")
                }
                if (jsonObject.has("resultDescription")) {
                    description = jsonObject.getString("resultDescription")
                }
                if (jsonObject.has("maskedCardNumber")) {
                    pan = jsonObject.getString("maskedCardNumber") + "\n"
                }
                if (jsonObject.has("dateOfTransaction")) {
                    transactionDate = jsonObject.getString("dateOfTransaction")
                    transactionTime = jsonObject.getString("timeOfTransaction")
                }
                amount = if (jsonObject.has("transactionAmount")) jsonObject.getString("transactionAmount") else "0"
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        
        return TransactionResult(
            status = status,
            amount = amount,
            responseCode = responseCode,
            merchantName = "",
            phoneNumber = "",
            maskPan = pan,
            merchantPhoneNumber = "",
            referenceNo = rrn,
            trace = stan,
            settleFailReason = description,
            terminalNo = terminalId,
            merchantId = merchantId,
            time = "$transactionDate $transactionTime"
        )
    } catch (e: Exception) {
        Log.e(TAG, "behTransactionDtoConverter: ${e.toString()}")
        Log.e(TAG, "behTransactionDtoConverter: ${e.cause}")
        Log.e(TAG, "behTransactionDtoConverter: ${e.message}")
        e.printStackTrace()
        return null
    }
}
