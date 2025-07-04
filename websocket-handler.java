// WebSocket Handler for Real-time Logs
// Add this to your Java HTTP server for real-time log streaming

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArraySet;
import java.security.MessageDigest;
import java.util.Base64;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class WebSocketHandler implements HttpHandler {
    private static final Set<WebSocketConnection> connections = new CopyOnWriteArraySet<>();
    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
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
        return "Upgrade".equalsIgnoreCase(connection) && "websocket".equalsIgnoreCase(upgrade);
    }

    private void handleWebSocketUpgrade(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        String acceptKey = generateAcceptKey(key);

        exchange.getResponseHeaders().set("Upgrade", "websocket");
        exchange.getResponseHeaders().set("Connection", "Upgrade");
        exchange.getResponseHeaders().set("Sec-WebSocket-Accept", acceptKey);
        exchange.sendResponseHeaders(101, 0);

        // Create WebSocket connection
        WebSocketConnection connection = new WebSocketConnection(exchange);
        connections.add(connection);
        
        // Handle WebSocket frames in a separate thread
        new Thread(() -> {
            try {
                connection.handleFrames();
            } catch (IOException e) {
                connections.remove(connection);
            }
        }).start();
    }

    private String generateAcceptKey(String key) {
        try {
            String combined = key + WEBSOCKET_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Broadcast log entry to all connected WebSocket clients
    public static void broadcastLog(RequestLog log) {
        String jsonLog = new Gson().toJson(log);
        for (WebSocketConnection connection : connections) {
            try {
                connection.sendMessage(jsonLog);
            } catch (IOException e) {
                connections.remove(connection);
            }
        }
    }

    static class WebSocketConnection {
        private final HttpExchange exchange;
        private final InputStream input;
        private final OutputStream output;

        public WebSocketConnection(HttpExchange exchange) {
            this.exchange = exchange;
            this.input = exchange.getRequestBody();
            this.output = exchange.getResponseBody();
        }

        public void sendMessage(String message) throws IOException {
            byte[] messageBytes = message.getBytes();
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
            output.write(frame.array(), 0, frame.position());
            output.flush();
        }

        public void handleFrames() throws IOException {
            // Handle incoming WebSocket frames
            // This is a simplified implementation
            byte[] buffer = new byte[1024];
            while (input.read(buffer) != -1) {
                // Process WebSocket frames
                // For this example, we mainly send data to client
            }
        }
    }
}
