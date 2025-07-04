// Static File Handler
package com.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private final String documentRoot;
    private final Map<String, String> mimeTypes;
    
    public StaticFileHandler(String documentRoot) {
        this.documentRoot = documentRoot;
        this.mimeTypes = initializeMimeTypes();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseHelper.addCORSHeaders(exchange);
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            ResponseHelper.sendOptionsResponse(exchange);
            return;
        }
        
        String requestPath = exchange.getRequestURI().getPath();
        
        // Remove /static prefix
        if (requestPath.startsWith("/static")) {
            requestPath = requestPath.substring(7);
        }
        
        // Default to index.html for root requests
        if (requestPath.equals("/") || requestPath.isEmpty()) {
            requestPath = "/index.html";
        }
        
        Path filePath = Paths.get(documentRoot, requestPath).normalize();
        
        // Security check - ensure file is within document root
        if (!filePath.startsWith(Paths.get(documentRoot).normalize())) {
            ResponseHelper.sendErrorResponse(exchange, 403, "Forbidden");
            return;
        }
        
        File file = filePath.toFile();
        
        if (!file.exists() || file.isDirectory()) {
            ResponseHelper.sendErrorResponse(exchange, 404, "File Not Found");
            return;
        }
        
        try {
            String contentType = getContentType(file.getName());
            byte[] fileContent = Files.readAllBytes(filePath);
            
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileContent.length));
            exchange.sendResponseHeaders(200, fileContent.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
            
        } catch (IOException e) {
            ResponseHelper.sendErrorResponse(exchange, 500, "Internal Server Error");
        }
    }
    
    private String getContentType(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        
        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }
    
    private Map<String, String> initializeMimeTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("html", "text/html; charset=UTF-8");
        types.put("htm", "text/html; charset=UTF-8");
        types.put("css", "text/css; charset=UTF-8");
        types.put("js", "application/javascript; charset=UTF-8");
        types.put("json", "application/json; charset=UTF-8");
        types.put("png", "image/png");
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("gif", "image/gif");
        types.put("svg", "image/svg+xml");
        types.put("ico", "image/x-icon");
        types.put("txt", "text/plain; charset=UTF-8");
        types.put("pdf", "application/pdf");
        types.put("zip", "application/zip");
        return types;
    }
}
