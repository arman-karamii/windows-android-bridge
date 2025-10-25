# WebSocketTestApp - BehPardakht Payment Integration

This Android app serves as a bridge between a WebSocket server and the BehPardakht payment application.

## Features

- **WebSocket Server**: Runs on port 8080 for real-time communication
- **BehPardakht Payment Integration**: Connects to BehPardakht app for card payments
- **Payment Result Handling**: Processes and displays payment results

## BehPardakht Integration Details

### Required Permissions
- `INTERNET` - For WebSocket server
- `ACCESS_NETWORK_STATE` - For network state monitoring

### Required Manifest Configuration
```xml
<queries>
    <package android:name="com.bpmellat.merchant" />
</queries>
```

### Payment Flow
1. User clicks "doPayment" button
2. App directly launches BehPardakht with payment data (amount: 25,000 Rials)
3. BehPardakht processes the payment
4. Returns result to bridge app
5. App parses and displays payment result

### Payment Data Structure
```json
{
    "applicationId": 10097,
    "printPaymentDetails": false,
    "saveDetail": false,
    "sessionId": "sessionId6284492603894537361",
    "totalAmount": "25000",
    "transactionType": "PURCHASE",
    "versionName": "1.0.0"
}
```

### Result Handling
- **Success**: `resultCode == "000"`
- **Failure**: Any other result code
- Results include: amount, response code, reference number, terminal ID, etc.

## Files Structure

- `MainActivity.kt` - Main activity with WebSocket server and payment handling
- `TransactionResult.kt` - Data class for payment results
- `CardPaymentUtil.kt` - Payment result parsing utilities
- `CAP.kt` - BehPardakht payment launching function
- `Constants.kt` - BehPardakht configuration constants

## Usage

1. Install the app on an Android device
2. Ensure BehPardakht app (`com.bpmellat.merchant`) is installed
3. Click "doPayment" to initiate a payment
4. Complete payment in BehPardakht app
5. View results in the app logs and toast messages

## WebSocket Endpoints

- `GET /health` - Health check
- `POST /pay/sale` - Payment request simulation
- `WebSocket /ws` - Real-time communication

## Notes

- The app uses the same payment integration pattern as the shoppin-android project
- BehPardakht app must be installed for payments to work
- Payment amounts are in Rials (25,000 Rials = 250.00)
- All payment events are logged for debugging
- No package checking is performed - the app directly attempts to launch BehPardakht
