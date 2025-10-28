package com.example.tindascan;

public class TransactionDetail {
    private String productName;
    private int quantity;
    private double priceAtSale;

    // Constructor
    public TransactionDetail(String productName, int quantity, double priceAtSale) {
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceAtSale() {
        return priceAtSale;
    }
}