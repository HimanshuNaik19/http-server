// Route Management System
package com.httpserver;

import com.sun.net.httpserver.HttpHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

public class RouteManager {
    private final Map<String, HttpHandler> routes = new ConcurrentHashMap<>();
    private final List<Route> routeList = new CopyOnWriteArrayList<>();
    
    public void addRoute(String path, String method, HttpHandler handler) {
        String key = method.toUpperCase() + ":" + path;
        routes.put(key, handler);
        
        Route route = new Route(path, handler.getClass().getSimpleName(), method, true);
        routeList.add(route);
    }
    
    public HttpHandler getHandler(String path, String method) {
        String key = method.toUpperCase() + ":" + path;
        return routes.get(key);
    }
    
    public List<Route> getAllRoutes() {
        return new ArrayList<>(routeList);
    }
    
    public boolean removeRoute(String path, String method) {
        String key = method.toUpperCase() + ":" + path;
        HttpHandler removed = routes.remove(key);
        
        if (removed != null) {
            routeList.removeIf(route -> 
                route.getPath().equals(path) && route.getMethod().equals(method));
            return true;
        }
        return false;
    }
    
    public void enableRoute(String path, String method, boolean enabled) {
        routeList.stream()
                .filter(route -> route.getPath().equals(path) && route.getMethod().equals(method))
                .findFirst()
                .ifPresent(route -> route.setEnabled(enabled));
    }
}

class Route {
    private final String path;
    private final String handler;
    private final String method;
    private boolean enabled;
    
    public Route(String path, String handler, String method, boolean enabled) {
        this.path = path;
        this.handler = handler;
        this.method = method;
        this.enabled = enabled;
    }
    
    // Getters and setters
    public String getPath() { return path; }
    public String getHandler() { return handler; }
    public String getMethod() { return method; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
