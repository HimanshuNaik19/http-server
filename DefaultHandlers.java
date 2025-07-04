// Default Route Handlers
package com.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Health Check Handler
class HealthCheckHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", System.currentTimeMillis());
        health.put("uptime", System.currentTimeMillis());
        health.put("version", "1.0.0");
        
        ResponseHelper.sendJsonResponse(exchange, health);
    }
}

// Test Handler
class TestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test endpoint working!");
        response.put("method", exchange.getRequestMethod());
        response.put("path", exchange.getRequestURI().getPath());
        response.put("query", exchange.getRequestURI().getQuery());
        response.put("headers", exchange.getRequestHeaders().entrySet());
        response.put("timestamp", System.currentTimeMillis());
        
        ResponseHelper.sendJsonResponse(exchange, response);
    }
}

// Server Control Handler
class ServerControlHandler implements HttpHandler {
    private final HttpServer server;
    private final String action;
    
    public ServerControlHandler(HttpServer server, String action) {
        this.server = server;
        this.action = action;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        if (!"POST".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if ("start".equals(action)) {
                // Server is already running if this endpoint is accessible
                response.put("message", "Server is already running");
                response.put("status", "running");
            } else if ("stop".equals(action)) {
                response.put("message", "Server stop requested");
                response.put("status", "stopping");
                
                // Send response first, then stop server
                ResponseHelper.sendJsonResponse(exchange, response);
                
                // Stop server in a separate thread to allow response to be sent
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Give time for response to be sent
                        server.stop();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                
                return; // Don't send response again
            } else {
                ResponseHelper.sendErrorResponse(exchange, 400, "Invalid action");
                return;
            }
            
            ResponseHelper.sendJsonResponse(exchange, response);
            
        } catch (Exception e) {
            ResponseHelper.sendErrorResponse(exchange, 500, "Server control failed: " + e.getMessage());
        }
    }
}
