package com.example.tindascan; // Or your model package

import java.util.Date; // If using Date for expiration

public class Product {
    private String name;
    private String barcode;
    private String category;
    private double sellingPrice;
    private double costPrice; // Optional
    private int stockQuantity;
    private int lowStockAlert; // Optional
    private String expirationDate; // Or use Date/LocalDate type

    // Constructor
    public Product(String name, String barcode, String category, double sellingPrice,
                   double costPrice, int stockQuantity, int lowStockAlert, String expirationDate) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.stockQuantity = stockQuantity;
        this.lowStockAlert = lowStockAlert;
        this.expirationDate = expirationDate;
    }

    // --- Getters (and maybe Setters if needed) ---
    public String getName() { return name; }
    public String getBarcode() { return barcode; }
    public String getCategory() { return category; }
    public double getSellingPrice() { return sellingPrice; }
    public double getCostPrice() { return costPrice; }
    public int getStockQuantity() { return stockQuantity; }
    public int getLowStockAlert() { return lowStockAlert; }
    public String getExpirationDate() { return expirationDate; }

    // --- You might add setters if you need to modify product details later ---
    // public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    // ... other setters ...
}