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
import java.time.Duration

data class SaleReq(val amount: Int, val orderId: String)
data class SaleRes(
    val status: String,
    val orderId: String,
    val authCode: String,
    val rrn: String,
    val scheme: String = "VISA"
)

class MainActivity : ComponentActivity() {

    private var server: ApplicationEngine? = null
    private val logMessages = mutableStateListOf<String>()
    
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
                    addLog("Payment request received from PC: Order ${req.orderId}, Amount ${req.amount}")
                    showToast("Payment request from PC: $${req.amount/100.0}")
                    
                    // Simulate processing delay
                    kotlinx.coroutines.delay(1500)
                    
                    val response = SaleRes(
                        status = "APPROVED",
                        orderId = req.orderId,
                        authCode = "123456",
                        rrn = "999000123456"
                    )
                    
                    addLog("Payment approved: ${response.status} - Auth: ${response.authCode}")
                    showToast("Payment approved: ${response.authCode}")
                    
                    call.respond(response)
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
                return
            }
            
            if (transaction.status == null || transaction.responseCode == null) {
                addLog("Invalid payment result data")
                showToast("Invalid payment result")
                return
            }
            
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
        }
    }
}
