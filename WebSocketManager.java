// WebSocket Manager for Real-time Communication
package com.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketManager implements HttpHandler {
    private final Set<WebSocketConnection> connections = new CopyOnWriteArraySet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();
    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            return;
        }
        
        if (isWebSocketUpgrade(exchange)) {
            handleWebSocketUpgrade(exchange);
        } else {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
        }
    }
    
    private boolean isWebSocketUpgrade(HttpExchange exchange) {
        String connection = exchange.getRequestHeaders().getFirst("Connection");
        String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
        return connection != null && connection.toLowerCase().contains("upgrade") && 
               "websocket".equalsIgnoreCase(upgrade);
    }
    
    private void handleWebSocketUpgrade(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        if (key == null) {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
            return;
        }
        
        String acceptKey = generateAcceptKey(key);
        
        exchange.getResponseHeaders().set("Upgrade", "websocket");
        exchange.getResponseHeaders().set("Connection", "Upgrade");
        exchange.getResponseHeaders().set("Sec-WebSocket-Accept", acceptKey);
        exchange.sendResponseHeaders(101, 0);
        
        WebSocketConnection connection = new WebSocketConnection(exchange);
        connections.add(connection);
        
        executor.submit(() -> {
            try {
                connection.handleConnection();
            } catch (Exception e) {
                System.err.println("WebSocket error: " + e.getMessage());
            } finally {
                connections.remove(connection);
            }
        });
    }
    
    private String generateAcceptKey(String key) {
        try {
            String combined = key + WEBSOCKET_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate WebSocket accept key", e);
        }
    }
    
    public void broadcastLog(RequestLog log) {
        String jsonLog = gson.toJson(log);
        connections.forEach(connection -> {
            try {
                connection.sendMessage(jsonLog);
            } catch (IOException e) {
                connections.remove(connection);
            }
        });
    }
    
    public void closeAllConnections() {
        connections.forEach(WebSocketConnection::close);
        connections.clear();
        executor.shutdown();
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
    
    private class WebSocketConnection {
        private final InputStream input;
        private final OutputStream output;
        private volatile boolean connected = true;
        
        public WebSocketConnection(HttpExchange exchange) {
            this.input = exchange.getRequestBody();
            this.output = exchange.getResponseBody();
        }
        
        public void handleConnection() throws IOException {
            byte[] buffer = new byte[1024];
            while (connected && input.read(buffer) != -1) {
                // Handle incoming WebSocket frames if needed
                // For this implementation, we mainly send data to clients
            }
        }
        
        public void sendMessage(String message) throws IOException {
            if (!connected) return;
            
            byte[] messageBytes = message.getBytes("UTF-8");
            ByteBuffer frame = ByteBuffer.allocate(messageBytes.length + 10);
            
            // WebSocket frame format
            frame.put((byte) 0x81); // FIN + text frame
            
            if (messageBytes.length < 126) {
                frame.put((byte) messageBytes.length);
            } else if (messageBytes.length < 65536) {
                frame.put((byte) 126);
                frame.putShort((short) messageBytes.length);
            } else {
                frame.put((byte) 127);
                frame.putLong(messageBytes.length);
            }
            
            frame.put(messageBytes);
            
            synchronized (output) {
                output.write(frame.array(), 0, frame.position());
                output.flush();
            }
        }
        
        public void close() {
            connected = false;
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }
}
