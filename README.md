# WebSocketTestApp - POS Bridge & Android Payment Server

A comprehensive Point of Sale (POS) bridge system that automatically discovers Android payment devices on local networks and processes payments through WebSocket communication. This project demonstrates real-time payment processing with automatic device discovery and failover capabilities.

## 🏗️ Architecture

This repository contains two main components:

### 1. POS Bridge Server (`pos-bridge/`)
- **Node.js/Express** HTTPS server with WebSocket support
- **Dynamic IP Detection**: Automatically scans 192.168.x.x network ranges
- **Device Health Monitoring**: Continuously monitors device availability
- **Automatic Failover**: Switches to alternative devices if current device fails
- **Web Interface**: Real-time status dashboard and payment testing
- **Self-signed Certificates**: HTTPS support for local development

### 2. Android Payment Server (`android/`)
- **Kotlin/Jetpack Compose** mobile application
- **Embedded Ktor Server**: Runs HTTP server on port 8080
- **Payment Simulation**: Simulates card payment processing with approval codes
- **WebSocket Support**: Real-time bidirectional communication
- **Health Endpoint**: Device discovery and monitoring
- **Modern UI**: Material Design 3 with real-time logging

## ✨ Key Features

- **🔍 Dynamic Device Discovery**: Automatically finds Android devices on local network
- **⚡ Real-time Communication**: WebSocket-based bidirectional messaging
- **🔄 Automatic Failover**: Seamless switching between available devices
- **🔒 Secure Communication**: HTTPS with self-signed certificates
- **📱 Modern Mobile UI**: Jetpack Compose with Material Design 3
- **🌐 Web Dashboard**: Real-time device status and payment testing interface
- **📊 Health Monitoring**: Continuous device availability tracking
- **🧪 Payment Simulation**: Complete payment flow with approval codes

## 🛠️ Technology Stack

### Backend (POS Bridge)
- **Node.js** - Runtime environment
- **Express.js** - Web framework
- **WebSocket (ws)** - Real-time communication
- **Axios** - HTTP client for device discovery
- **CORS** - Cross-origin resource sharing
- **Self-signed** - Certificate generation

### Android App
- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Ktor Server** - Embedded HTTP server
- **Coroutines** - Asynchronous programming
- **Material Design 3** - UI components

### Communication
- **WebSocket** - Real-time bidirectional communication
- **HTTP REST APIs** - Payment processing endpoints
- **HTTPS** - Secure communication protocol

## 🚀 Quick Start

### Prerequisites
- **Node.js** (v14 or higher)
- **Android Studio** (for Android development)
- **Android device** or emulator
- **Local network** (192.168.x.x range)

### 1. Setup POS Bridge Server

```bash
# Navigate to pos-bridge directory
cd pos-bridge

# Install dependencies
npm install

# Start the server
npm start
# or for development with auto-restart
npm run dev
```

The bridge will be available at: `https://localhost:6743`

### 2. Setup Android Payment Server

```bash
# Navigate to android directory
cd android

# Open in Android Studio or build with Gradle
./gradlew assembleDebug

# Install on Android device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run the System

1. **Start Android App**: Launch the payment app on your Android device
2. **Start POS Bridge**: Run `npm start` in the pos-bridge directory
3. **Open Web Interface**: Navigate to `https://localhost:6743`
4. **Connect**: Click "Connect" to establish WebSocket connection
5. **Discover Devices**: Click "Scan Devices" to find Android devices
6. **Test Payment**: Click "Start Payment" to process a test payment

## 📱 Android App Features

- **Payment Server**: Runs HTTP server on port 8080
- **Health Endpoint**: `GET /health` returns `{"ok": true}`
- **Payment Endpoint**: `POST /pay/sale` accepts `{amount, orderId}`
- **WebSocket Endpoint**: `/ws` for real-time communication
- **Real-time Logging**: Live log messages with timestamps
- **Toast Notifications**: Payment status notifications
- **Modern UI**: Material Design 3 with Jetpack Compose

## 🌐 Web Interface Features

- **Device Status**: Real-time display of discovered devices
- **Connection Management**: WebSocket connection status
- **Payment Testing**: Interactive payment processing
- **Manual Discovery**: Trigger device scanning on demand
- **Error Handling**: Clear error messages and recovery suggestions
- **Responsive Design**: Works on desktop and mobile browsers

## 🔧 Configuration

### POS Bridge Configuration (`server.js`)
```javascript
const PORT = 6743;                    // HTTPS server port
const ANDROID_PORT = 8080;            // Android app port
const DISCOVERY_TIMEOUT = 5000;        // Device health check timeout
const DISCOVERY_INTERVAL = 30000;      // Auto-scan interval (disabled)
```

### Android App Configuration (`MainActivity.kt`)
```kotlin
// Server runs on port 8080, listening on all interfaces
embeddedServer(CIO, host = "0.0.0.0", port = 8080)
```

## 📋 API Endpoints

### Android Payment Server
- `GET /health` - Health check endpoint
- `POST /pay/sale` - Process payment request
- `WebSocket /ws` - Real-time communication

### POS Bridge Server
- `WebSocket /` - Main communication channel
- `GET /` - Web interface
- `Static files` - CSS, JS, HTML assets

## 🔍 Network Discovery

The POS bridge automatically discovers Android devices by:

1. **Network Interface Analysis**: Examines local network interfaces for 192.168.x.x addresses
2. **Range Scanning**: If no interfaces found, scans common ranges (192.168.1, 192.168.0, etc.)
3. **IP Scanning**: Tests IPs 1-254 in each detected range
4. **Health Checks**: Validates Android server health endpoint
5. **Device Management**: Maintains list of discovered devices with status

## 🛡️ Security Considerations

- **Self-signed Certificates**: Generated automatically for HTTPS
- **CORS Configuration**: Limited to localhost origins
- **Network Scope**: Scanning limited to 192.168.x.x ranges
- **Local Network Only**: No authentication required (suitable for local use)
- **Firewall**: Ensure port 8080 is accessible on Android device

## 🐛 Troubleshooting

### Common Issues

**No devices found**
- Ensure Android app is running and accessible on port 8080
- Check network connectivity and firewall settings
- Verify both devices are on the same 192.168.x.x network

**Connection failed**
- Check WebSocket connection in browser developer tools
- Verify HTTPS certificate acceptance (self-signed)
- Ensure POS bridge server is running on port 6743

**Payment failed**
- Verify Android app is responding to health checks
- Check Android device logs for error messages
- Ensure payment endpoint is accessible

**Multiple devices**
- Bridge automatically selects the first available device
- Use manual scan to discover alternative devices
- Check device status in web interface

## 📁 Project Structure

```
WebSocketTestApp/
├── pos-bridge/                 # POS Bridge Server
│   ├── server.js              # Main server file
│   ├── package.json           # Node.js dependencies
│   ├── public/                # Web interface files
│   │   └── index.html         # Main web page
│   └── README.md              # POS Bridge documentation
├── android/                   # Android Payment Server
│   ├── app/                   # Android application
│   │   ├── src/main/java/     # Kotlin source code
│   │   │   └── com/example/paymenttempapp/
│   │   │       └── MainActivity.kt  # Main activity
│   │   ├── build.gradle.kts   # Build configuration
│   │   └── src/main/res/      # Android resources
│   ├── build.gradle.kts       # Project build config
│   └── gradle/                # Gradle wrapper
└── README.md                  # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the ISC License - see the [LICENSE](LICENSE) file for details.

## 🎯 Use Cases

- **POS Integration Testing**: Test payment processing workflows
- **Payment Device Management**: Discover and manage multiple payment devices
- **WebSocket Communication**: Research and development of real-time communication
- **Local Network Payment Processing**: Simulate payment processing in local environments
- **Mobile Payment Server Development**: Develop and test mobile payment solutions
- **Network Device Discovery**: Implement automatic device discovery systems

## 🔮 Future Enhancements

- [ ] Add authentication and authorization
- [ ] Implement payment method selection
- [ ] Add transaction history and reporting
- [ ] Support for multiple payment providers
- [ ] Enhanced error handling and recovery
- [ ] Performance monitoring and metrics
- [ ] Docker containerization
- [ ] CI/CD pipeline setup

## 📞 Support

For support, email your-email@example.com or create an issue in the repository.

---

**Note**: This is a development and testing tool. For production use, implement proper security measures, authentication, and compliance with payment industry standards.