package com.example.paymenttempapp

data class TransactionResult(
    val status: Int?,
    var amount: String?,
    val responseCode: String?,
    val merchantName: String?,
    val merchantPhoneNumber: String?,
    val phoneNumber: String?,
    val maskPan: String?,
    val referenceNo: String?,
    val trace: String?,
    val settleFailReason: String?,
    val terminalNo: String?,
    val merchantId: String?,
    val time: String?
) {
    fun isSuccessTransaction(): Boolean {
        // BehPardakht success code is "000"
        return this.responseCode == "000"
    }
}
