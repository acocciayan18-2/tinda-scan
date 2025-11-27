package com.example.tindascan;

public class Transaction {
    private long id;
    private String timestamp;
    private double totalAmount;

    // Constructor
    public Transaction(long id, String timestamp, double totalAmount) {
        this.id = id;
        this.timestamp = timestamp;
        this.totalAmount = totalAmount;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}