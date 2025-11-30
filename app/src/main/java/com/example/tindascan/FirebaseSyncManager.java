package com.example.tindascan;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {

    private FirebaseFirestore db;
    private final DatabaseHelper dbHelper;
    private final Context context;

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public FirebaseSyncManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            this.db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseSync", "Init failed", e);
        }
    }

    // Helper to get the current logged-in user's ID
    private String getUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "offline_user"; // Fallback if tested without login
    }

    // ==========================================
    // 📤 EXPORT ALL DATA
    // ==========================================
    public void exportAllData(SyncCallback callback) {
        if (db == null) {
            callback.onFailure("Firebase not initialized.");
            return;
        }

        WriteBatch batch = db.batch();
        String currentUserId = getUserId(); // Get ID once for this operation

        // 1. Export Products
        List<Product> products = dbHelper.getAllProductsList();
        CollectionReference productsRef = db.collection("users").document(currentUserId).collection("products");
        for (Product p : products) {
            // Use Barcode or generate a unique ID
            String docId = (p.getBarcode() != null && !p.getBarcode().isEmpty()) ? p.getBarcode() : "no_barcode_" + p.getName();

            Map<String, Object> map = new HashMap<>();
            map.put("name", p.getName());
            map.put("category", p.getCategory());
            map.put("barcode", p.getBarcode());
            map.put("selling_price", p.getSellingPrice());
            map.put("cost_price", p.getCostPrice());
            map.put("stock_quantity", p.getStockQuantity());
            map.put("weight", p.getWeight());
            map.put("expiration_date", p.getExpirationDate());
            map.put("low_stock_alert", p.getLowStockAlert());

            batch.set(productsRef.document(docId), map);
        }

        // 2. Export Transactions
        List<Map<String, Object>> transactions = dbHelper.getTransactionsForBackup();
        CollectionReference trxRef = db.collection("users").document(currentUserId).collection("transactions");
        for (Map<String, Object> t : transactions) {
            // Use ID as document name to prevent duplicates
            batch.set(trxRef.document(String.valueOf(t.get("transaction_id"))), t);
        }

        // 3. Export Transaction Details
        List<Map<String, Object>> details = dbHelper.getTransactionDetailsForBackup();
        CollectionReference detailsRef = db.collection("users").document(currentUserId).collection("transaction_details");
        for (Map<String, Object> d : details) {
            batch.set(detailsRef.document(String.valueOf(d.get("detail_id"))), d);
        }

        // 4. Commit All
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Backup complete! (" + products.size() + " products, " + transactions.size() + " orders)"))
                .addOnFailureListener(e -> callback.onFailure("Backup failed: " + e.getMessage()));
    }

    // ==========================================
    // 📥 IMPORT ALL DATA
    // ==========================================
    public void importAllData(SyncCallback callback) {
        if (db == null) {
            callback.onFailure("Firebase not initialized.");
            return;
        }

        // We download sequentially to avoid complexity
        importProducts(callback);
    }

    private void importProducts(SyncCallback callback) {
        String currentUserId = getUserId();

        db.collection("users").document(currentUserId).collection("products").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        try {
                            // Basic parsing safely
                            String name = doc.getString("name");
                            String barcode = doc.getString("barcode");
                            String category = doc.getString("category");
                            String weight = doc.getString("weight");
                            String expiry = doc.getString("expiration_date");

                            double selling = doc.getDouble("selling_price") != null ? doc.getDouble("selling_price") : 0;
                            double cost = doc.getDouble("cost_price") != null ? doc.getDouble("cost_price") : 0;
                            int stock = doc.getLong("stock_quantity") != null ? doc.getLong("stock_quantity").intValue() : 0;
                            int low = doc.getLong("low_stock_alert") != null ? doc.getLong("low_stock_alert").intValue() : 5;

                            Product p = new Product(name, barcode, weight, category, selling, cost, stock, low, expiry);
                            dbHelper.upsertProduct(p);
                        } catch (Exception e) { Log.e("Import", "Product error", e); }
                    }
                    // Next step: Import Transactions
                    importTransactions(callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Product import failed: " + e.getMessage()));
    }

    private void importTransactions(SyncCallback callback) {
        String currentUserId = getUserId();

        db.collection("users").document(currentUserId).collection("transactions").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        if (doc.getData() != null) dbHelper.restoreTransaction(doc.getData());
                    }
                    // Next step: Import Details
                    importTransactionDetails(callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Transaction import failed"));
    }

    private void importTransactionDetails(SyncCallback callback) {
        String currentUserId = getUserId();

        db.collection("users").document(currentUserId).collection("transaction_details").get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        if (doc.getData() != null) dbHelper.restoreTransactionDetail(doc.getData());
                    }
                    callback.onSuccess("Full restore complete!");
                })
                .addOnFailureListener(e -> callback.onFailure("Detail import failed"));
    }
}