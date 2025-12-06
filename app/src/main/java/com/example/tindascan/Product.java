package com.example.tindascan;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Product implements Serializable {
    private int id;                 // for SQLite primary key
    private String name;
    private String category;
    private String barcode;
    private double sellingPrice;    // ✅ Consolidated from 'price' and 'sellingPrice'
    private double costPrice;
    private int lowStockAlert;      // ✅ Changed from double to int
    private int stockQuantity;
    private String weight;          // optional (e.g. "500g", "1kg")
    private String expirationDate;  // optional

    /**
     * Constructor 1: For reading a full product from the database
     */
    public Product(int id, String name, String category, String barcode, double sellingPrice,
                   int stockQuantity, String weight, String expirationDate,
                   double costPrice, int lowStockAlert) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.barcode = barcode;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
        this.weight = weight;
        this.expirationDate = expirationDate;
        this.costPrice = costPrice;
        this.lowStockAlert = lowStockAlert;
    }

    /**
     * Constructor 2: For creating a new product (e.g., from AddProductDialog)
     * No 'id' because the database will auto-generate it.
     */
    public Product(String name, String barcode, String weight, String category, double sellingPrice,
                   double costPrice, int stockQuantity, int lowStockAlert, String expirationDate) {
        this.name = name;
        this.barcode = barcode;
        this.weight = weight;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.stockQuantity = stockQuantity;
        this.lowStockAlert = lowStockAlert;
        this.expirationDate = expirationDate;
    }

    /**
     * Constructor 3: For loading products into the Stock.java list
     */
    public Product(int id, String name, String category, String barcode, double sellingPrice, int stockQuantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.barcode = barcode;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
    }

    /**
     * Constructor 4: For getProductByBarcode (matches DatabaseHelper)
     * This is a simplified version of Constructor 1.
     */
    public Product(int id, String name, String category, String barcode, double sellingPrice,
                   int stockQuantity, String weight, String expirationDate) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.barcode = barcode;
        this.sellingPrice = sellingPrice;
        this.stockQuantity = stockQuantity;
        this.weight = weight;
        this.expirationDate = expirationDate;
    }

    public Product() {
        // Empty constructor needed by Gson
    }

    public long getExpirationDateMillis() {
        if (expirationDate == null || expirationDate.isEmpty()) return Long.MAX_VALUE;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = sdf.parse(expirationDate);
            if (date != null) return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Long.MAX_VALUE;
    }



    // --- Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getBarcode() { return barcode; }
    public double getPrice() { return sellingPrice; }
    public double getSellingPrice() { return sellingPrice; }
    public double getCostPrice() { return costPrice; }
    public int getLowStockAlert() { return lowStockAlert; }
    public int getStockQuantity() { return stockQuantity; }
    public String getWeight() { return weight; }
    public String getExpirationDate() { return expirationDate; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setPrice(double price) { this.sellingPrice = price; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public void setLowStockAlert(int lowStockAlert) { this.lowStockAlert = lowStockAlert; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setWeight(String weight) { this.weight = weight; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
}