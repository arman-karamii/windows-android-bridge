package com.example.paymenttempapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.json.JSONException
import org.json.JSONObject

fun callBehPardakhtPay(payableAmount: String, context: Context, onStartPay: () -> Unit, onCallPay: () -> Unit) {
    val intent = Intent("com.bpmellat.merchant")
    val bundle = Bundle()
    val jsonObject = JSONObject()
    
    try {
        jsonObject.put("applicationId", Constants.BEHPARDAKHT_APPLICATION_ID)
        jsonObject.put("printPaymentDetails", false)
        jsonObject.put("saveDetail", false)
        jsonObject.put("sessionId", "sessionId6284492603894537361")
        jsonObject.put("totalAmount", payableAmount)
        jsonObject.put("transactionType", Constants.BEHPARDAKHT_TRANSACTION_TYPE)
        jsonObject.put("versionName", Constants.BEHPARDAKHT_VERSION_NAME)
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    
    bundle.putString("PaymentData", jsonObject.toString())
    intent.putExtras(bundle)
    
    // Call the callback before starting payment
    onStartPay()
    
    // Launch the payment activity
    (context as MainActivity).resultLauncher.launch(intent)
}
