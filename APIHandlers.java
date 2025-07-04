// API Request Handlers
package com.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Server Status Handler
class ServerStatusHandler implements HttpHandler {
    private final ServerStatsManager statsManager;
    private final Gson gson = new Gson();
    
    public ServerStatusHandler(ServerStatsManager statsManager) {
        this.statsManager = statsManager;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", statsManager.isServerRunning() ? "running" : "stopped");
        response.put("timestamp", System.currentTimeMillis());
        
        ResponseHelper.sendJsonResponse(exchange, response);
    }
}

// Server Stats Handler
class ServerStatsHandler implements HttpHandler {
    private final ServerStatsManager statsManager;
    private final Gson gson = new Gson();
    
    public ServerStatsHandler(ServerStatsManager statsManager) {
        this.statsManager = statsManager;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("uptime", statsManager.getFormattedUptime());
        stats.put("totalRequests", statsManager.getTotalRequests());
        stats.put("activeConnections", statsManager.getActiveConnections());
        stats.put("memoryUsage", statsManager.getFormattedMemoryUsage());
        stats.put("cpuUsage", statsManager.getCpuUsage());
        
        ResponseHelper.sendJsonResponse(exchange, stats);
    }
}

// Logs Handler
class LogsHandler implements HttpHandler {
    private final RequestLogger requestLogger;
    private final Gson gson = new Gson();
    
    public LogsHandler(RequestLogger requestLogger) {
        this.requestLogger = requestLogger;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        String method = exchange.getRequestMethod();
        
        if ("GET".equals(method)) {
            String query = exchange.getRequestURI().getQuery();
            int limit = 50; // default
            
            if (query != null && query.contains("limit=")) {
                try {
                    limit = Integer.parseInt(query.split("limit=")[1].split("&")[0]);
                } catch (NumberFormatException e) {
                    limit = 50;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", requestLogger.getLogs(limit));
            response.put("total", requestLogger.getLogCount());
            
            ResponseHelper.sendJsonResponse(exchange, response);
            
        } else if ("DELETE".equals(method)) {
            requestLogger.clearLogs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logs cleared successfully");
            
            ResponseHelper.sendJsonResponse(exchange, response);
        } else {
            ResponseHelper.sendErrorResponse(exchange, 405, "Method Not Allowed");
        }
    }
}

// Routes Handler
class RoutesHandler implements HttpHandler {
    private final RouteManager routeManager;
    private final Gson gson = new Gson();
    
    public RoutesHandler(RouteManager routeManager) {
        this.routeManager = routeManager;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        String method = exchange.getRequestMethod();
        
        if ("GET".equals(method)) {
            Map<String, Object> response = new HashMap<>();
            response.put("routes", routeManager.getAllRoutes());
            
            ResponseHelper.sendJsonResponse(exchange, response);
            
        } else if ("POST".equals(method)) {
            String body = ResponseHelper.readRequestBody(exchange);
            
            try {
                Map<String, String> routeData = gson.fromJson(body, Map.class);
                String path = routeData.get("path");
                String handler = routeData.get("handler");
                String routeMethod = routeData.get("method");
                
                if (path != null && handler != null && routeMethod != null) {
                    // Create a simple handler for the new route
                    HttpHandler newHandler = new CustomRouteHandler(handler);
                    routeManager.addRoute(path, routeMethod, newHandler);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Route added successfully");
                    response.put("path", path);
                    response.put("handler", handler);
                    response.put("method", routeMethod);
                    
                    ResponseHelper.sendJsonResponse(exchange, response);
                } else {
                    ResponseHelper.sendErrorResponse(exchange, 400, "Missing required fields");
                }
            } catch (Exception e) {
                ResponseHelper.sendErrorResponse(exchange, 400, "Invalid JSON");
            }
        } else {
            ResponseHelper.sendErrorResponse(exchange, 405, "Method Not Allowed");
        }
    }
}

// Server Config Handler
class ServerConfigHandler implements HttpHandler {
    private final int port;
    private final Gson gson = new Gson();
    
    public ServerConfigHandler(int port) {
        this.port = port;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("port", port);
        config.put("documentRoot", "./static");
        config.put("defaultIndex", "index.html");
        config.put("maxConnections", 100);
        config.put("threadPoolSize", 10);
        
        ResponseHelper.sendJsonResponse(exchange, config);
    }
}

// Custom Route Handler
class CustomRouteHandler implements HttpHandler {
    private final String handlerName;
    
    public CustomRouteHandler(String handlerName) {
        this.handlerName = handlerName;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Response from " + handlerName);
        response.put("path", exchange.getRequestURI().getPath());
        response.put("method", exchange.getRequestMethod());
        response.put("timestamp", System.currentTimeMillis());
        
        ResponseHelper.sendJsonResponse(exchange, response);
    }
}
