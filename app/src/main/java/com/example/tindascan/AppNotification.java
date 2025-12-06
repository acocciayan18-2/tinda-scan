package com.example.tindascan;

public class AppNotification {
    private int id;
    private String title;
    private String message;
    private String type;
    private String timestamp;
    private boolean isRead;

    public AppNotification(int id, String title, String message, String type, String timestamp, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
}