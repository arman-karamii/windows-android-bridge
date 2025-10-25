// server.js
import fs from "fs";
import path from "path";
import https from "https";
import express from "express";
import cors from "cors";
import { WebSocketServer } from "ws";
import selfsigned from "selfsigned";
import axios from "axios";
import { networkInterfaces } from "os";

const PORT = 6743;
const DATA_DIR = path.resolve("./.data");
const CERT_PATH = path.join(DATA_DIR, "cert.pem");
const KEY_PATH = path.join(DATA_DIR, "key.pem");

// Device discovery configuration
const ANDROID_PORT = 8080;
const DISCOVERY_TIMEOUT = 5000; // 5 seconds
const DISCOVERY_INTERVAL = 30000; // 30 seconds
let discoveredDevices = new Map(); // IP -> { lastSeen, status }
let currentDevice = null;

// Ensure .data exists
fs.mkdirSync(DATA_DIR, { recursive: true });

// Network discovery functions
function getLocalNetworkRanges() {
  const interfaces = networkInterfaces();
  const ranges = [];
  
  for (const [name, addresses] of Object.entries(interfaces)) {
    for (const addr of addresses) {
      if (addr.family === 'IPv4' && !addr.internal) {
        const ip = addr.address;
        if (ip.startsWith('192.168.')) {
          // Extract network prefix (192.168.x)
          const parts = ip.split('.');
          ranges.push(`${parts[0]}.${parts[1]}.${parts[2]}`);
        }
      }
    }
  }
  
  // If no 192.168.x found, scan common ranges
  if (ranges.length === 0) {
    ranges.push('192.168.1', '192.168.0', '192.168.100', '192.168.141');
  }
  
  return [...new Set(ranges)]; // Remove duplicates
}

async function scanDevice(ip) {
  try {
    const response = await axios.get(`http://${ip}:${ANDROID_PORT}/health`, {
      timeout: DISCOVERY_TIMEOUT,
      validateStatus: (status) => status === 200
    });
    
    if (response.data && response.data.ok) {
      discoveredDevices.set(ip, {
        lastSeen: Date.now(),
        status: 'online',
        health: response.data
      });
      console.log(`✓ Android device found at ${ip}:${ANDROID_PORT}`);
      return ip;
    }
  } catch (error) {
    // Device not responding or not Android server
    discoveredDevices.set(ip, {
      lastSeen: Date.now(),
      status: 'offline',
      error: error.message
    });
  }
  return null;
}

async function discoverAndroidDevices() {
  const ranges = getLocalNetworkRanges();
  console.log(`Scanning network ranges: ${ranges.join(', ')}`);
  
  const promises = [];
  
  for (const range of ranges) {
    // Scan IPs 1-254 in each range
    for (let i = 1; i <= 254; i++) {
      const ip = `${range}.${i}`;
      promises.push(scanDevice(ip));
    }
  }
  
  const results = await Promise.allSettled(promises);
  const foundDevices = results
    .map(result => result.status === 'fulfilled' ? result.value : null)
    .filter(ip => ip !== null);
  
  if (foundDevices.length > 0) {
    console.log(`Found ${foundDevices.length} Android device(s): ${foundDevices.join(', ')}`);
    
    // Use the first found device or switch if current device is offline
    if (!currentDevice || !discoveredDevices.get(currentDevice)?.status === 'online') {
      currentDevice = foundDevices[0];
      console.log(`Selected device: ${currentDevice}`);
    }
  } else {
    console.log('No Android devices found on network');
    currentDevice = null;
  }
  
  return foundDevices;
}

// Manual device discovery only - no automatic scanning

// Create or load self-signed cert for localhost
function getCert() {
  if (fs.existsSync(CERT_PATH) && fs.existsSync(KEY_PATH)) {
    return {
      key: fs.readFileSync(KEY_PATH),
      cert: fs.readFileSync(CERT_PATH),
    };
  }
  const attrs = [{ name: "commonName", value: "localhost" }];
  const pems = selfsigned.generate(attrs, { days: 3650 });
  fs.writeFileSync(CERT_PATH, pems.cert);
  fs.writeFileSync(KEY_PATH, pems.private);
  return { key: Buffer.from(pems.private), cert: Buffer.from(pems.cert) };
}

const app = express();
app.use(express.json());

// CORS for dev — narrow this later to your real PWA origin
app.use(
  cors({
    origin: [/^https?:\/\/localhost(:\d+)?$/],
    credentials: true,
  })
);

// Serve a tiny test client
app.use(express.static(path.join(process.cwd(), "public")));

const server = https.createServer(getCert(), app);
const wss = new WebSocketServer({ server });

wss.on("connection", (ws, req) => {
  console.log("WS connected:", req.socket.remoteAddress);
  
  // Send current device status to client
  ws.send(JSON.stringify({ 
    type: "DEVICE_STATUS", 
    currentDevice,
    discoveredDevices: Array.from(discoveredDevices.entries()).map(([ip, data]) => ({
      ip,
      status: data.status,
      lastSeen: data.lastSeen
    }))
  }));
  
  ws.on("message", async (raw) => {
    try {
      const msg = JSON.parse(raw.toString());
      
      if (msg.action === "START_PAYMENT") {
        console.log("Payment request:", msg);
        
        if (!currentDevice) {
          ws.send(JSON.stringify({ 
            type: "ERROR", 
            message: "No Android device found. Please click 'Scan Devices' to discover devices." 
          }));
          return;
        }
        
        try {
          const terminalBase = `http://${currentDevice}:${ANDROID_PORT}`;
          const payload = { amount: msg.amount, timeout: msg.timeout || 120000 };
          
          console.log(`Sending payment to ${terminalBase}/pay/sale`);
          const res = await axios.post(`${terminalBase}/pay/sale`, payload, { timeout: 150000 });
          ws.send(JSON.stringify({ type: "RESULT", ...res.data }));
        } catch (error) {
          console.error("Payment API error:", error.message);
          
          // If current device failed, suggest manual scan
          if (error.code === 'ECONNREFUSED' || error.code === 'ETIMEDOUT') {
            console.log("Current device failed, please scan for alternatives");
            ws.send(JSON.stringify({ 
              type: "ERROR", 
              message: `Device ${currentDevice} failed. Please click 'Scan Devices' to find alternatives.` 
            }));
            return;
          }
          
          ws.send(JSON.stringify({ 
            type: "ERROR", 
            message: `Payment failed: ${error.message}` 
          }));
        }
      } else if (msg.action === "SCAN_DEVICES") {
        // Manual device scan request
        const devices = await discoverAndroidDevices();
        ws.send(JSON.stringify({ 
          type: "DEVICE_STATUS", 
          currentDevice,
          discoveredDevices: Array.from(discoveredDevices.entries()).map(([ip, data]) => ({
            ip,
            status: data.status,
            lastSeen: data.lastSeen
          }))
        }));
      }
    } catch (e) {
      console.error("WS message error:", e);
      ws.send(JSON.stringify({ type: "ERROR", message: e.message }));
    }
  });
  ws.on("close", () => console.log("WS closed"));
});

server.listen(PORT, () => {
  console.log(`Bridge up at https://localhost:${PORT}`);
});
