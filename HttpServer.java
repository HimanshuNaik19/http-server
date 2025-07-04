// Main HTTP Server Class
package com.httpserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpServer {
    private HttpServer server;
    private final int port;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final RequestLogger requestLogger;
    private final RouteManager routeManager;
    private final StaticFileHandler staticFileHandler;
    private final WebSocketManager webSocketManager;
    private final ServerStatsManager statsManager;
    
    public HttpServer(int port) {
        this.port = port;
        this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        this.requestLogger = new RequestLogger();
        this.routeManager = new RouteManager();
        this.staticFileHandler = new StaticFileHandler("./static");
        this.webSocketManager = new WebSocketManager();
        this.statsManager = new ServerStatsManager();
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(threadPoolExecutor);
        
        // Setup API endpoints
        setupAPIEndpoints();
        
        // Setup WebSocket endpoint
        server.createContext("/ws/logs", webSocketManager);
        
        // Setup static file serving
        server.createContext("/static", staticFileHandler);
        
        // Setup custom routes
        setupCustomRoutes();
        
        // Main request handler with logging
        server.createContext("/", new MainRequestHandler());
        
        server.start();
        statsManager.setServerStartTime(System.currentTimeMillis());
        System.out.println("Server started on port " + port);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            threadPoolExecutor.shutdown();
            webSocketManager.closeAllConnections();
            System.out.println("Server stopped");
        }
    }
    
    private void setupAPIEndpoints() {
        // Server management endpoints
        server.createContext("/api/server/status", new ServerStatusHandler(statsManager));
        server.createContext("/api/server/stats", new ServerStatsHandler(statsManager));
        server.createContext("/api/server/start", new ServerControlHandler(this, "start"));
        server.createContext("/api/server/stop", new ServerControlHandler(this, "stop"));
        server.createContext("/api/server/config", new ServerConfigHandler(port));
        
        // Logging endpoints
        server.createContext("/api/logs", new LogsHandler(requestLogger));
        
        // Route management endpoints
        server.createContext("/api/routes", new RoutesHandler(routeManager));
    }
    
    private void setupCustomRoutes() {
        // Add default routes
        routeManager.addRoute("/health", "GET", new HealthCheckHandler());
        routeManager.addRoute("/api/test", "GET", new TestHandler());
    }
    
    // Main request handler that logs all requests
    private class MainRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            try {
                // Check if route exists in route manager
                HttpHandler handler = routeManager.getHandler(path, method);
                if (handler != null) {
                    handler.handle(exchange);
                } else {
                    // Return 404 for unknown routes
                    ResponseHelper.sendErrorResponse(exchange, 404, "Not Found");
                }
            } catch (Exception e) {
                ResponseHelper.sendErrorResponse(exchange, 500, "Internal Server Error");
            } finally {
                // Log the request
                long responseTime = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponseCode();
                
                RequestLog log = new RequestLog(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    statusCode,
                    responseTime,
                    exchange.getRemoteAddress().getAddress().getHostAddress(),
                    exchange.getRequestHeaders().getFirst("User-Agent")
                );
                
                requestLogger.logRequest(log);
                statsManager.incrementRequestCount();
                
                // Broadcast to WebSocket clients
                webSocketManager.broadcastLog(log);
            }
        }
    }
    
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        
        HttpServer server = new HttpServer(port);
        
        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
