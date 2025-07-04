// Java HTTP Server API Endpoints
// Add these endpoints to your Java HTTP server

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.gson.Gson;

public class ServerAPIEndpoints {
    private static final Gson gson = new Gson();
    private static final Queue<RequestLog> requestLogs = new ConcurrentLinkedQueue<>();
    private static final List<Route> routes = new ArrayList<>();
    private static ServerStats serverStats = new ServerStats();
    private static boolean serverRunning = true;
    private static final long startTime = System.currentTimeMillis();

    public static void setupAPIEndpoints(HttpServer server) {
        // CORS handler wrapper
        HttpHandler corsHandler = (HttpHandler) exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }
        };

        // Server status endpoint
        server.createContext("/api/server/status", exchange -> {
            corsHandler.handle(exchange);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", serverRunning ? "running" : "stopped");
            response.put("timestamp", LocalDateTime.now().toString());
            
            sendJsonResponse(exchange, response);
        });

        // Server statistics endpoint
        server.createContext("/api/server/stats", exchange -> {
            corsHandler.handle(exchange);
            
            long uptime = System.currentTimeMillis() - startTime;
            String uptimeStr = formatUptime(uptime);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("uptime", uptimeStr);
            stats.put("totalRequests", serverStats.totalRequests);
            stats.put("activeConnections", serverStats.activeConnections);
            stats.put("memoryUsage", formatMemoryUsage());
            stats.put("cpuUsage", getCpuUsage());
            
            sendJsonResponse(exchange, stats);
        });

        // Request logs endpoint
        server.createContext("/api/logs", exchange -> {
            corsHandler.handle(exchange);
            
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, Object> response = new HashMap<>();
                response.put("logs", new ArrayList<>(requestLogs));
                sendJsonResponse(exchange, response);
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                requestLogs.clear();
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Logs cleared successfully");
                sendJsonResponse(exchange, response);
            }
        });

        // Routes management endpoint
        server.createContext("/api/routes", exchange -> {
            corsHandler.handle(exchange);
            
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, Object> response = new HashMap<>();
                response.put("routes", routes);
                sendJsonResponse(exchange, response);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Route newRoute = gson.fromJson(body, Route.class);
                newRoute.enabled = true;
                routes.add(newRoute);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Route added successfully");
                response.put("route", newRoute);
                sendJsonResponse(exchange, response);
            }
        });

        // Server configuration endpoint
        server.createContext("/api/server/config", exchange -> {
            corsHandler.handle(exchange);
            
            Map<String, Object> config = new HashMap<>();
            config.put("port", server.getAddress().getPort());
            config.put("documentRoot", "/static");
            config.put("defaultIndex", "index.html");
            
            sendJsonResponse(exchange, config);
        });

        // Server control endpoints
        server.createContext("/api/server/start", exchange -> {
            corsHandler.handle(exchange);
            serverRunning = true;
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Server started successfully");
            response.put("status", "running");
            sendJsonResponse(exchange, response);
        });

        server.createContext("/api/server/stop", exchange -> {
            corsHandler.handle(exchange);
            serverRunning = false;
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Server stopped successfully");
            response.put("status", "stopped");
            sendJsonResponse(exchange, response);
        });
    }

    // Log incoming requests
    public static void logRequest(HttpExchange exchange, long responseTime, int statusCode) {
        RequestLog log = new RequestLog();
        log.id = UUID.randomUUID().toString();
        log.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        log.method = exchange.getRequestMethod();
        log.path = exchange.getRequestURI().getPath();
        log.status = statusCode;
        log.responseTime = responseTime;
        log.clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        log.userAgent = exchange.getRequestHeaders().getFirst("User-Agent");

        requestLogs.offer(log);
        if (requestLogs.size() > 100) {
            requestLogs.poll(); // Keep only last 100 logs
        }

        serverStats.totalRequests++;
        
        // Broadcast to WebSocket clients if implemented
        broadcastLogToWebSocket(log);
    }

    private static void sendJsonResponse(HttpExchange exchange, Object data) throws IOException {
        String jsonResponse = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes());
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        Scanner scanner = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private static String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private static String formatMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return String.format("%.1f MB", usedMemory / (1024.0 * 1024.0));
    }

    private static double getCpuUsage() {
        // Simplified CPU usage - you might want to use a more sophisticated method
        return Math.random() * 100; // Placeholder
    }

    private static void broadcastLogToWebSocket(RequestLog log) {
        // Implement WebSocket broadcasting here
        // This would send the log entry to all connected WebSocket clients
    }

    // Data classes
    static class RequestLog {
        String id;
        String timestamp;
        String method;
        String path;
        int status;
        long responseTime;
        String clientIp;
        String userAgent;
    }

    static class Route {
        String path;
        String handler;
        String method;
        boolean enabled;
    }

    static class ServerStats {
        int totalRequests = 0;
        int activeConnections = 0;
    }
}
