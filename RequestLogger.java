// Request Logging System
package com.httpserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestLogger {
    private final Queue<RequestLog> logs = new ConcurrentLinkedQueue<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int MAX_LOGS = 1000;
    
    public void logRequest(RequestLog log) {
        lock.writeLock().lock();
        try {
            logs.offer(log);
            // Keep only the most recent logs
            while (logs.size() > MAX_LOGS) {
                logs.poll();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public List<RequestLog> getLogs(int limit) {
        lock.readLock().lock();
        try {
            return logs.stream()
                    .limit(limit)
                    .collect(ArrayList::new, (list, log) -> list.add(0, log), (list1, list2) -> {
                        list2.addAll(list1);
                        return list2;
                    });
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void clearLogs() {
        lock.writeLock().lock();
        try {
            logs.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int getLogCount() {
        return logs.size();
    }
}

class RequestLog {
    private final String id;
    private final String timestamp;
    private final String method;
    private final String path;
    private final int status;
    private final long responseTime;
    private final String clientIp;
    private final String userAgent;
    
    public RequestLog(String method, String path, int status, long responseTime, 
                     String clientIp, String userAgent) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.method = method;
        this.path = path;
        this.status = status;
        this.responseTime = responseTime;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
    
    // Getters
    public String getId() { return id; }
    public String getTimestamp() { return timestamp; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public long getResponseTime() { return responseTime; }
    public String getClientIp() { return clientIp; }
    public String getUserAgent() { return userAgent; }
}
