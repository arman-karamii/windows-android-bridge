package com.example.paymenttempapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

data class SaleReq(val amount: Int, val timeout: Long = 120000L)
data class SaleRes(
    val status: String,
    val authCode: String,
    val rrn: String
)

class MainActivity : ComponentActivity() {

    private var server: ApplicationEngine? = null
    private val logMessages = mutableStateListOf<String>()
    
    // Single payment synchronization mechanism
    private var currentPaymentDeferred: CompletableDeferred<TransactionResult>? = null
    
    /**
     * Activity result launcher for handling payment results from BehPardakht app
     */
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultBEH = result.data?.extras?.getString("PaymentResult")
                handleTransactionResultBehPardakht(resultBEH ?: "")
            } else {
                addLog("Payment cancelled or failed")
                showToast("Payment cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "P10 Payment Server",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Server running on: http://0.0.0.0:8080",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(onClick = {
                    // Trigger BehPardakht payment
                    val payableAmount = "25000" // 1000.00 in Rials (example amount)
                    callBehPardakhtPay(
                        payableAmount = payableAmount,
                        context = context,
                        onStartPay = {
                            addLog("Starting BehPardakht payment for amount: $payableAmount")
                            showToast("Starting payment...")
                        }
                    ) {
                        addLog("Payment completed")
                    }
                }) {
                    Text("doPayment")
                }
                
                
                Text(
                    text = "Log Messages:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logMessages) { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            startServer()
        }
    }
    
    private fun addLog(message: String) {
        logMessages.add(0, "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())} - $message")
        if (logMessages.size > 50) {
            logMessages.removeAt(logMessages.size - 1)
        }
    }
    
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun startServer() = withContext(Dispatchers.IO) {
        // Listen on all interfaces, port 8080
        server = embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
            install(ContentNegotiation) { jackson() }
            install(CallLogging)
            
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
            }

            routing {
                get("/health") {
                    addLog("Health check requested")
                    call.respond(mapOf("ok" to true))
                }
                post("/pay/sale") {
                    val req = call.receive<SaleReq>()
                    addLog("Payment request received from PC: Amount ${req.amount} Rials")
                    showToast("Payment request from PC: ${req.amount} Rials")
                    
                    // Check if payment is already in progress
                    if (currentPaymentDeferred != null) {
                        addLog("Payment already in progress, rejecting new request")
                        call.respond(SaleRes(
                            status = "REJECTED",
                            authCode = "",
                            rrn = ""
                        ))
                        return@post
                    }
                    
                    // Amount is already in Rials, convert to string for BehPardakht
                    val payableAmount = req.amount.toString()
                    
                    // Create deferred for this payment
                    val deferred = CompletableDeferred<TransactionResult>()
                    currentPaymentDeferred = deferred
                    
                    try {
                        // Trigger BehPardakht payment
                        callBehPardakhtPay(
                            payableAmount = payableAmount,
                            context = this@MainActivity,
                            onStartPay = {
                                addLog("Starting BehPardakht payment for amount: $payableAmount")
                                showToast("Starting payment...")
                            }
                        ) {
                            addLog("Payment completed")
                        }
                        
                        // Wait for payment result with timeout
                        val transactionResult = withTimeoutOrNull(req.timeout) {
                            deferred.await()
                        }
                        
                        val response = if (transactionResult != null) {
                            if (transactionResult.isSuccessTransaction()) {
                                addLog("Payment SUCCESS: Amount=${transactionResult.amount}, Auth=${transactionResult.responseCode}, RRN=${transactionResult.referenceNo}")
                                SaleRes(
                                    status = "APPROVED",
                                    authCode = transactionResult.responseCode ?: "",
                                    rrn = transactionResult.referenceNo ?: ""
                                )
                            } else {
                                addLog("Payment FAILED: Code=${transactionResult.responseCode}, Reason=${transactionResult.settleFailReason}")
                                SaleRes(
                                    status = "DECLINED",
                                    authCode = transactionResult.responseCode ?: "",
                                    rrn = transactionResult.referenceNo ?: ""
                                )
                            }
                        } else {
                            addLog("Payment timeout after ${req.timeout}ms")
                            SaleRes(
                                status = "TIMEOUT",
                                authCode = "",
                                rrn = ""
                            )
                        }
                        
                        addLog("Payment response: ${response.status} - Auth: ${response.authCode}")
                        showToast("Payment ${response.status.lowercase()}: ${response.authCode}")
                        
                        call.respond(response)
                        
                    } finally {
                        // Clean up
                        currentPaymentDeferred = null
                    }
                }
                webSocket("/ws") {
                    addLog("WebSocket connection established")
                    showToast("WebSocket connected")
                    
                    // On connect, send hello
                    send("""{"type":"HELLO"}""")
                    
                    // Echo or push events as needed
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val txt = frame.readText()
                            addLog("WebSocket message received: $txt")
                            showToast("WebSocket message: ${txt.take(30)}...")
                            // handle text frames if you wish
                            outgoing.send(Frame.Text("""{"type":"ECHO","data":$txt}"""))
                        }
                    }
                }
            }
        }.start(wait = false)
        
        addLog("Ktor server started on http://0.0.0.0:8080")
        showToast("Payment server started!")
    }

    override fun onDestroy() {
        server?.stop(500, 1000)
        super.onDestroy()
    }
    
    /**
     * Handles the transaction result from BehPardakht payment app
     */
    private fun handleTransactionResultBehPardakht(result: String) {
        try {
            val transaction = behTransactionDtoConverter(result)
            
            if (transaction == null) {
                addLog("Failed to parse payment result")
                showToast("Payment result parsing failed")
                // Complete with error if payment is pending
                currentPaymentDeferred?.completeExceptionally(Exception("Failed to parse payment result"))
                return
            }
            
            if (transaction.status == null || transaction.responseCode == null) {
                addLog("Invalid payment result data")
                showToast("Invalid payment result")
                // Complete with error if payment is pending
                currentPaymentDeferred?.completeExceptionally(Exception("Invalid payment result data"))
                return
            }
            
            // Complete the pending payment request
            currentPaymentDeferred?.complete(transaction)
            
            lifecycleScope.launch {
                if (transaction.isSuccessTransaction()) {
                    addLog("Payment SUCCESS: Amount=${transaction.amount}, Auth=${transaction.responseCode}, RRN=${transaction.referenceNo}")
                    showToast("Payment successful! Amount: ${transaction.amount}")
                } else {
                    addLog("Payment FAILED: Code=${transaction.responseCode}, Reason=${transaction.settleFailReason}")
                    showToast("Payment failed: ${transaction.settleFailReason}")
                }
            }
        } catch (e: Exception) {
            addLog("Error handling payment result: ${e.message}")
            showToast("Error processing payment result")
            // Complete with error if payment is pending
            currentPaymentDeferred?.completeExceptionally(e)
        }
    }
}
