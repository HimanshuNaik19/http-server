// Server Statistics Manager
package com.httpserver;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServerStatsManager {
    private final AtomicLong serverStartTime = new AtomicLong(0);
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private volatile boolean serverRunning = true;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    public void setServerStartTime(long startTime) {
        serverStartTime.set(startTime);
    }
    
    public void incrementRequestCount() {
        totalRequests.incrementAndGet();
    }
    
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }
    
    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }
    
    public void setServerRunning(boolean running) {
        this.serverRunning = running;
    }
    
    public boolean isServerRunning() {
        return serverRunning;
    }
    
    public int getTotalRequests() {
        return totalRequests.get();
    }
    
    public int getActiveConnections() {
        return activeConnections.get();
    }
    
    public long getUptimeMillis() {
        return serverStartTime.get() > 0 ? System.currentTimeMillis() - serverStartTime.get() : 0;
    }
    
    public String getFormattedUptime() {
        long uptimeMs = getUptimeMillis();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public String getFormattedMemoryUsage() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        return String.format("%.1f MB", usedMemory / (1024.0 * 1024.0));
    }
    
    public double getCpuUsage() {
        // Simple CPU usage estimation
        // In production, you might want to use a more sophisticated method
        return Math.random() * 100; // Placeholder
    }
    
    public void reset() {
        totalRequests.set(0);
        activeConnections.set(0);
        serverStartTime.set(System.currentTimeMillis());
    }
}
