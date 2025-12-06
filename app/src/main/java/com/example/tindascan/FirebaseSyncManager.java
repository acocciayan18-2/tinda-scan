package com.example.tindascan;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {

    private FirebaseFirestore db;
    private final DatabaseHelper dbHelper;
    private final Context context;
    private final Handler mainHandler;

    private volatile boolean isCancelled = false;

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
        void onProgress(String status, int percent);
    }

    public FirebaseSyncManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.mainHandler = new Handler(Looper.getMainLooper());

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            this.db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("FirebaseSync", "Init failed", e);
        }
    }

    private String getUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "offline_user";
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    public void cancelProcess() {
        this.isCancelled = true;
    }

    // Helper to run callbacks on Main Thread
    private void runOnUI(Runnable action) {
        mainHandler.post(action);
    }

    // EXPORT ALL DATA (BACKUP)
    public void exportAllData(SyncCallback callback) {
        isCancelled = false;

        if (!isNetworkAvailable()) {
            callback.onFailure("No internet connection. Backup cancelled.");
            return;
        }
        if (db == null) {
            callback.onFailure("Firebase not initialized.");
            return;
        }

        // Run preparation in background to avoid UI lag
        new Thread(() -> {
            try {
                runOnUI(() -> callback.onProgress("Reading local database...", 0));

                List<Product> products = dbHelper.getAllProductsList();
                List<Map<String, Object>> transactions = dbHelper.getTransactionsForBackup();
                List<Map<String, Object>> details = dbHelper.getTransactionDetailsForBackup();

                int totalItems = products.size() + transactions.size() + details.size();
                if (totalItems == 0) {
                    runOnUI(() -> callback.onSuccess("Nothing to backup."));
                    return;
                }

                WriteBatch batch = db.batch();
                String currentUserId = getUserId();
                int currentProgress = 0;

                // Products
                CollectionReference productsRef = db.collection("users").document(currentUserId).collection("products");
                for (Product p : products) {
                    if (isCancelled) { runOnUI(() -> callback.onFailure("Backup Cancelled.")); return; }

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

                    currentProgress++;
                    int pct = (currentProgress * 100) / totalItems;
                    runOnUI(() -> callback.onProgress("Packing Products...", pct));
                }

                // Transactions
                CollectionReference trxRef = db.collection("users").document(currentUserId).collection("transactions");
                for (Map<String, Object> t : transactions) {
                    if (isCancelled) { runOnUI(() -> callback.onFailure("Backup Cancelled.")); return; }
                    batch.set(trxRef.document(String.valueOf(t.get("transaction_id"))), t);

                    currentProgress++;
                    int pct = (currentProgress * 100) / totalItems;
                    runOnUI(() -> callback.onProgress("Packing History...", pct));
                }

                // Details
                CollectionReference detailsRef = db.collection("users").document(currentUserId).collection("transaction_details");
                for (Map<String, Object> d : details) {
                    if (isCancelled) { runOnUI(() -> callback.onFailure("Backup Cancelled.")); return; }
                    batch.set(detailsRef.document(String.valueOf(d.get("detail_id"))), d);

                    currentProgress++;
                    int pct = (currentProgress * 100) / totalItems;
                    runOnUI(() -> callback.onProgress("Packing Details...", pct));
                }

                if (isCancelled) {
                    runOnUI(() -> callback.onFailure("Backup Cancelled."));
                    return;
                }

                runOnUI(() -> callback.onProgress("Uploading to cloud...", 100));

                batch.commit()
                        .addOnSuccessListener(aVoid -> {
                            if (!isCancelled) {
                                dbHelper.logActivity("BACKUP", "Exported data to cloud");

                                // 🔥 Update Timestamp
                                new NotificationUtils(context).updateLastBackupTimestamp();

                                callback.onSuccess("Backup complete!");
                            } else {
                                runOnUI(() -> callback.onFailure("Backup Cancelled."));
                            }
                        })
                        .addOnFailureListener(e -> runOnUI(() -> callback.onFailure("Backup failed: " + e.getMessage())));

            } catch (Exception e) {
                runOnUI(() -> callback.onFailure("Error: " + e.getMessage()));
            }
        }).start();
    }


    //  IMPORT ALL DATA (RESTORE WITH ROLLBACK)

    public void importAllData(SyncCallback callback) {
        isCancelled = false;

        if (!isNetworkAvailable()) {
            callback.onFailure("No internet connection. Restore cancelled.");
            return;
        }
        if (db == null) {
            callback.onFailure("Firebase not initialized.");
            return;
        }

        String uid = getUserId();
        callback.onProgress("Downloading data...", 0);

        com.google.android.gms.tasks.Task<QuerySnapshot> t1 = db.collection("users").document(uid).collection("products").get();
        com.google.android.gms.tasks.Task<QuerySnapshot> t2 = db.collection("users").document(uid).collection("transactions").get();
        com.google.android.gms.tasks.Task<QuerySnapshot> t3 = db.collection("users").document(uid).collection("transaction_details").get();

        Tasks.whenAllSuccess(t1, t2, t3).addOnSuccessListener(results -> {
            if (isCancelled) { callback.onFailure("Restore Cancelled."); return; }

            QuerySnapshot pSnap = (QuerySnapshot) results.get(0);
            QuerySnapshot tSnap = (QuerySnapshot) results.get(1);
            QuerySnapshot dSnap = (QuerySnapshot) results.get(2);

            int totalItems = pSnap.size() + tSnap.size() + dSnap.size();

            if (totalItems == 0) {
                callback.onFailure("No backup found online.");
                return;
            }

            new Thread(() -> saveDataToLocalDb(pSnap, tSnap, dSnap, totalItems, callback)).start();

        }).addOnFailureListener(e -> callback.onFailure("Download Error: " + e.getMessage()));
    }

    private void saveDataToLocalDb(QuerySnapshot pSnap, QuerySnapshot tSnap, QuerySnapshot dSnap, int totalItems, SyncCallback callback) {
        dbHelper.beginTransaction();
        int currentProgress = 0;

        try {
            // 1. Process Products
            for (DocumentSnapshot doc : pSnap) {
                if (isCancelled) throw new RuntimeException("Cancelled");

                // Parsing
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
                dbHelper.upsertProduct(p, false); // Silent update

                currentProgress++;
                int pct = (currentProgress * 100) / totalItems;
                runOnUI(() -> callback.onProgress("Restoring Products...", pct));
            }

            // 2. Process Transactions
            for (DocumentSnapshot doc : tSnap) {
                if (isCancelled) throw new RuntimeException("Cancelled");
                if (doc.getData() != null) dbHelper.restoreTransaction(doc.getData());

                currentProgress++;
                int pct = (currentProgress * 100) / totalItems;
                runOnUI(() -> callback.onProgress("Restoring History...", pct));
            }

            // 3. Process Details
            for (DocumentSnapshot doc : dSnap) {
                if (isCancelled) throw new RuntimeException("Cancelled");
                if (doc.getData() != null) dbHelper.restoreTransactionDetail(doc.getData());

                currentProgress++;
                int pct = (currentProgress * 100) / totalItems;
                runOnUI(() -> callback.onProgress("Restoring Details...", pct));
            }

            // Commit Changes
            dbHelper.setTransactionSuccessful();
            dbHelper.logActivity("RESTORE", "Restored data from cloud");
            runOnUI(() -> callback.onSuccess("Restore Complete!"));

        } catch (RuntimeException e) {
            if ("Cancelled".equals(e.getMessage())) {
                runOnUI(() -> callback.onFailure("Restore Cancelled. Rolling back..."));
            } else {
                runOnUI(() -> callback.onFailure("Database Error: " + e.getMessage()));
            }
        } finally {
            dbHelper.endTransaction();
        }
    }
}