package com.example.tindascan;

public class ActivityLog {
    private String type;    // "SALE", "ADD", "EDIT", "DELETE", "BACKUP", "RESTORE"
    private String details;
    private String timestamp;

    public ActivityLog(String type, String details, String timestamp) {
        this.type = type;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getType() { return type; }
    public String getDetails() { return details; }
    public String getTimestamp() { return timestamp; }
}