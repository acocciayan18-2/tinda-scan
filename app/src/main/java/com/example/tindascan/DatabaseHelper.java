package com.example.tindascan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tindascan.db";
    private static final int DATABASE_VERSION = 6; // Version 6 for Notifications Table

    private final Context context;

    // --- Common Column ---
    private static final String COLUMN_USER_ID = "user_id";

    // --- Product Table ---
    private static final String TABLE_PRODUCTS = "products";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_BARCODE = "barcode";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_SELLING_PRICE = "selling_price";
    private static final String COLUMN_COST_PRICE = "cost_price";
    private static final String COLUMN_STOCK_QTY = "stock_quantity";
    private static final String COLUMN_LOW_STOCK_ALERT = "low_stock_alert";
    private static final String COLUMN_EXPIRATION_DATE = "expiration_date";

    // --- Activity Logs Table ---
    private static final String TABLE_ACTIVITY_LOGS = "activity_logs";
    private static final String COLUMN_LOG_ID = "log_id";
    private static final String COLUMN_LOG_TYPE = "type";
    private static final String COLUMN_LOG_DETAILS = "details";
    private static final String COLUMN_LOG_TIMESTAMP = "timestamp";

    // --- Transaction Tables ---
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_TRX_ID = "transaction_id";
    private static final String COLUMN_TRX_TIMESTAMP = "timestamp";
    private static final String COLUMN_TRX_TOTAL_AMOUNT = "total_amount";

    private static final String TABLE_TRANSACTION_DETAILS = "transaction_details";
    private static final String COLUMN_DETAIL_ID = "detail_id";
    private static final String COLUMN_DETAIL_TRX_ID = "transaction_id";
    private static final String COLUMN_DETAIL_PRODUCT_BARCODE = "product_barcode";
    private static final String COLUMN_DETAIL_PRODUCT_NAME = "product_name_at_sale";
    private static final String COLUMN_DETAIL_QUANTITY_SOLD = "quantity_sold";
    private static final String COLUMN_DETAIL_PRICE_AT_SALE = "price_at_sale";

    // --- Notifications Table ---
    private static final String TABLE_NOTIFICATIONS = "notifications";
    private static final String COLUMN_NOTIF_ID = "notif_id";
    private static final String COLUMN_NOTIF_TITLE = "title";
    private static final String COLUMN_NOTIF_MESSAGE = "message";
    private static final String COLUMN_NOTIF_TYPE = "type";
    private static final String COLUMN_NOTIF_IS_READ = "is_read";
    private static final String COLUMN_NOTIF_TIMESTAMP = "timestamp";

    // --- SQL Creation Strings ---
    private static final String CREATE_TABLE_PRODUCTS =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_WEIGHT + " TEXT, " +
                    COLUMN_BARCODE + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_SELLING_PRICE + " REAL, " +
                    COLUMN_COST_PRICE + " REAL, " +
                    COLUMN_STOCK_QTY + " INTEGER DEFAULT 0, " +
                    COLUMN_LOW_STOCK_ALERT + " INTEGER DEFAULT 5, " +
                    COLUMN_EXPIRATION_DATE + " TEXT" +
                    ");";

    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_TRX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_TRX_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_TRX_TOTAL_AMOUNT + " REAL NOT NULL" +
                    ");";

    private static final String CREATE_TABLE_TRANSACTION_DETAILS =
            "CREATE TABLE " + TABLE_TRANSACTION_DETAILS + " (" +
                    COLUMN_DETAIL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_DETAIL_TRX_ID + " INTEGER NOT NULL, " +
                    COLUMN_DETAIL_PRODUCT_BARCODE + " TEXT, " +
                    COLUMN_DETAIL_PRODUCT_NAME + " TEXT NOT NULL, " +
                    COLUMN_DETAIL_QUANTITY_SOLD + " INTEGER NOT NULL, " +
                    COLUMN_DETAIL_PRICE_AT_SALE + " REAL NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_DETAIL_TRX_ID + ") REFERENCES " + TABLE_TRANSACTIONS + "(" + COLUMN_TRX_ID + ")" +
                    ");";

    private static final String CREATE_TABLE_ACTIVITY_LOGS =
            "CREATE TABLE " + TABLE_ACTIVITY_LOGS + " (" +
                    COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_LOG_TYPE + " TEXT NOT NULL, " +
                    COLUMN_LOG_DETAILS + " TEXT, " +
                    COLUMN_LOG_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_TABLE_NOTIFICATIONS =
            "CREATE TABLE " + TABLE_NOTIFICATIONS + " (" +
                    COLUMN_NOTIF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_NOTIF_TITLE + " TEXT, " +
                    COLUMN_NOTIF_MESSAGE + " TEXT, " +
                    COLUMN_NOTIF_TYPE + " TEXT, " +
                    COLUMN_NOTIF_IS_READ + " INTEGER DEFAULT 0, " +
                    COLUMN_NOTIF_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PRODUCTS);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_TRANSACTION_DETAILS);
        db.execSQL(CREATE_TABLE_ACTIVITY_LOGS);
        db.execSQL(CREATE_TABLE_NOTIFICATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION_DETAILS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
        onCreate(db);
    }

    private String getCurrentUserId() {
        if (context != null && FirebaseApp.getApps(context).isEmpty()) {
            return "offline_user";
        }
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                return user.getUid();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Firebase Auth Error", e);
        }
        return "offline_user";
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================

    public void saveNotification(String title, String message, String type) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, getCurrentUserId());
            values.put(COLUMN_NOTIF_TITLE, title);
            values.put(COLUMN_NOTIF_MESSAGE, message);
            values.put(COLUMN_NOTIF_TYPE, type);
            values.put(COLUMN_NOTIF_IS_READ, 0);
            db.insert(TABLE_NOTIFICATIONS, null, values);
        } catch (Exception e) {
            Log.e("DB", "Failed to save notification", e);
        }
    }

    public List<AppNotification> getAllNotifications() {
        List<AppNotification> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NOTIFICATIONS +
                        " WHERE " + COLUMN_USER_ID + " = ? ORDER BY " + COLUMN_NOTIF_TIMESTAMP + " DESC",
                new String[]{getCurrentUserId()});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_TITLE));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_MESSAGE));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_TYPE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_TIMESTAMP));
                boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIF_IS_READ)) == 1;

                list.add(new AppNotification(id, title, message, type, time, isRead));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public int getUnreadNotificationCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS +
                        " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_NOTIF_IS_READ + " = 0",
                new String[]{getCurrentUserId()});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void markAllNotificationsAsRead() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIF_IS_READ, 1);
        db.update(TABLE_NOTIFICATIONS, values, COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
    }

    public void deleteNotification(int notificationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATIONS, COLUMN_NOTIF_ID + " = ?", new String[]{String.valueOf(notificationId)});
    }

    public List<Product> getLowStockProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS +
                        " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_STOCK_QTY + " <= " + COLUMN_LOW_STOCK_ALERT +
                        " AND " + COLUMN_STOCK_QTY + " > 0",
                new String[]{getCurrentUserId()});

        if (cursor.moveToFirst()) {
            do { list.add(mapCursorToProduct(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Product> getOutOfStockProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS +
                        " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_STOCK_QTY + " <= 0",
                new String[]{getCurrentUserId()});

        if (cursor.moveToFirst()) {
            do { list.add(mapCursorToProduct(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Product> getExpiringProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE " + COLUMN_USER_ID + " = ? " +
                " AND " + COLUMN_EXPIRATION_DATE + " <= date('now', '+7 days')" +
                " AND " + COLUMN_EXPIRATION_DATE + " >= date('now')";

        Cursor cursor = db.rawQuery(query, new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do { list.add(mapCursorToProduct(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private Product mapCursorToProduct(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
        String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE));
        double sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SELLING_PRICE));
        int stockQty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STOCK_QTY));
        String weight = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
        String expirationDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE));

        double costPrice = 0;
        if(cursor.getColumnIndex("cost_price") != -1) costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
        int lowStockAlert = 5;
        if(cursor.getColumnIndex("low_stock_alert") != -1) lowStockAlert = cursor.getInt(cursor.getColumnIndexOrThrow("low_stock_alert"));

        return new Product(id, name, category, barcode, sellingPrice, stockQty, weight, expirationDate, costPrice, lowStockAlert);
    }

    // ==========================================
    // ACTIVITY LOG METHODS
    // ==========================================

    public void logActivity(String type, String details) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, getCurrentUserId());
            values.put(COLUMN_LOG_TYPE, type);
            values.put(COLUMN_LOG_DETAILS, details);
            db.insert(TABLE_ACTIVITY_LOGS, null, values);
        } catch (Exception e) {
            Log.e("DB", "Failed to log activity", e);
        }
    }

    public List<ActivityLog> getRecentActivities() {
        List<ActivityLog> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_ACTIVITY_LOGS +
                        " WHERE " + COLUMN_USER_ID + " = ? " +
                        " ORDER BY " + COLUMN_LOG_TIMESTAMP + " DESC LIMIT 20",
                new String[]{getCurrentUserId()}
        );

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TYPE));
                String details = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DETAILS));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_TIMESTAMP));
                logs.add(new ActivityLog(type, details, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return logs;
    }

    public void clearAllActivities() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ACTIVITY_LOGS, COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
    }

    // ==========================================
    // PRODUCT METHODS
    // ==========================================

    public boolean addProduct(Product product) {
        return addProduct(product, true);
    }

    public boolean addProduct(Product product, boolean shouldLog) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, getCurrentUserId());
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_WEIGHT, product.getWeight());
        values.put(COLUMN_BARCODE, product.getBarcode());
        values.put(COLUMN_CATEGORY, product.getCategory());
        values.put(COLUMN_SELLING_PRICE, product.getSellingPrice());
        values.put(COLUMN_COST_PRICE, product.getCostPrice());
        values.put(COLUMN_STOCK_QTY, product.getStockQuantity());
        values.put(COLUMN_LOW_STOCK_ALERT, product.getLowStockAlert());
        values.put(COLUMN_EXPIRATION_DATE, product.getExpirationDate());

        long result = db.insert(TABLE_PRODUCTS, null, values);
        if (result != -1 && shouldLog) {
            logActivity("ADD", "Added: " + product.getName());
        }
        return result != -1;
    }

    public Cursor getAllProducts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
    }

    // 🔥 UPDATED: Methods to return BOOLEAN correctly
    public boolean updateProduct(Product product) {
        return updateProductByBarcode(product, true);
    }

    public boolean updateProduct(Product product, boolean shouldLog) {
        return updateProductByBarcode(product, shouldLog);
    }

    public boolean deleteProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows;
        String userId = getCurrentUserId();

        if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
            rows = db.delete(TABLE_PRODUCTS,
                    COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                    new String[]{product.getBarcode().trim(), userId});
        } else {
            rows = db.delete(TABLE_PRODUCTS,
                    COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                    new String[]{String.valueOf(product.getId()), userId});
        }

        if (rows > 0) {
            logActivity("DELETE", "Deleted product: " + product.getName());
        }
        return rows > 0;
    }

    public boolean deleteProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS,
                COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{barcode.trim(), getCurrentUserId()});

        if (rows > 0) {
            logActivity("DELETE", "Deleted product: " + barcode);
        }
        return rows > 0;
    }

    public boolean productExists(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{barcode, getCurrentUserId()});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public Product getProductByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{barcode, getCurrentUserId()});

        if (cursor.moveToFirst()) {
            return mapCursorToProduct(cursor);
        }
        cursor.close();
        return null;
    }

    public List<Product> getProductsByNameOrBarcode(String query) {
        List<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String userId = getCurrentUserId();
        String searchPattern = "%" + query.trim() + "%";

        String sql = "SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE (" + COLUMN_NAME + " LIKE ? OR " + COLUMN_BARCODE + " LIKE ?) " +
                " AND " + COLUMN_USER_ID + " = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{searchPattern, searchPattern, userId});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                productList.add(mapCursorToProduct(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return productList;
    }

    public int getStockQuantityByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_STOCK_QTY + " FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{barcode, getCurrentUserId()});
        if (cursor.moveToFirst()) {
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STOCK_QTY));
            cursor.close();
            return stock;
        }
        cursor.close();
        return -1;
    }

    // ==========================================
    // TRANSACTION METHODS
    // ==========================================

    public boolean recordSale(List<CartItem> cartItems, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        String userId = getCurrentUserId();
        boolean success = false;

        try {
            ContentValues trxValues = new ContentValues();
            trxValues.put(COLUMN_USER_ID, userId);
            trxValues.put(COLUMN_TRX_TOTAL_AMOUNT, totalAmount);
            long transactionId = db.insert(TABLE_TRANSACTIONS, null, trxValues);
            if (transactionId == -1) return false;

            for (CartItem item : cartItems) {
                Product product = item.getProduct();
                if (product == null) continue;

                int quantitySold = item.getQuantity();
                String barcode = product.getBarcode();

                int currentDbStock = getStockQuantityByBarcode(barcode);
                if (currentDbStock == -1) continue;

                int newStock = Math.max(0, currentDbStock - quantitySold);

                ContentValues detailValues = new ContentValues();
                detailValues.put(COLUMN_USER_ID, userId);
                detailValues.put(COLUMN_DETAIL_TRX_ID, transactionId);
                detailValues.put(COLUMN_DETAIL_PRODUCT_BARCODE, barcode);
                detailValues.put(COLUMN_DETAIL_PRODUCT_NAME, product.getName());
                detailValues.put(COLUMN_DETAIL_QUANTITY_SOLD, quantitySold);
                detailValues.put(COLUMN_DETAIL_PRICE_AT_SALE, product.getPrice());
                db.insert(TABLE_TRANSACTION_DETAILS, null, detailValues);

                ContentValues updateValues = new ContentValues();
                updateValues.put(COLUMN_STOCK_QTY, newStock);
                db.update(TABLE_PRODUCTS, updateValues,
                        COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                        new String[]{barcode, userId});
            }

            db.setTransactionSuccessful();
            logActivity("SALE", "Sold items. Total: ₱" + totalAmount);
            success = true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }

        // Trigger Notifications
        if (success) {
            try {
                NotificationUtils notif = new NotificationUtils(context);
                notif.checkInventoryNotifications(this);
                notif.showDailyReport(getTodaySalesTotal(), 0);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Notification error", e);
            }
        }
        return success;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_USER_ID + " = ? ORDER BY " + COLUMN_TRX_TIMESTAMP + " DESC",
                new String[]{getCurrentUserId()}
        );
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRX_ID));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRX_TIMESTAMP));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRX_TOTAL_AMOUNT));
                transactionList.add(new Transaction(id, timestamp, total));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactionList;
    }

    public List<TransactionDetail> getTransactionDetails(long transactionId) {
        List<TransactionDetail> detailList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                COLUMN_DETAIL_PRODUCT_NAME + ", " +
                COLUMN_DETAIL_QUANTITY_SOLD + ", " +
                COLUMN_DETAIL_PRICE_AT_SALE +
                " FROM " + TABLE_TRANSACTION_DETAILS +
                " WHERE " + COLUMN_DETAIL_TRX_ID + " = ? AND " + COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(transactionId), getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRODUCT_NAME));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_QUANTITY_SOLD));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRICE_AT_SALE));
                detailList.add(new TransactionDetail(name, qty, price));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return detailList;
    }

    // --- Dashboard Stats ---

    public double getTodaySalesTotal() {
        double totalSales = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + COLUMN_TRX_TOTAL_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                " WHERE date(" + COLUMN_TRX_TIMESTAMP + ", 'localtime') = date('now', 'localtime') AND " + COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) totalSales = cursor.getDouble(0);
        cursor.close();
        return totalSales;
    }

    public double getTodayProfit() {
        double totalProfit = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM( (d." + COLUMN_DETAIL_PRICE_AT_SALE + " - p." + COLUMN_COST_PRICE + ") * d." + COLUMN_DETAIL_QUANTITY_SOLD + " ) " +
                "FROM " + TABLE_TRANSACTION_DETAILS + " d " +
                "JOIN " + TABLE_TRANSACTIONS + " t ON d." + COLUMN_DETAIL_TRX_ID + " = t." + COLUMN_TRX_ID + " " +
                "JOIN " + TABLE_PRODUCTS + " p ON d." + COLUMN_DETAIL_PRODUCT_BARCODE + " = p." + COLUMN_BARCODE + " " +
                "WHERE date(t." + COLUMN_TRX_TIMESTAMP + ", 'localtime') = date('now', 'localtime') " +
                "AND d." + COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) totalProfit = cursor.getDouble(0);
        cursor.close();
        return totalProfit;
    }

    public int getTotalUniqueProducts() {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getLowStockCount() {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS +
                " WHERE " + COLUMN_STOCK_QTY + " <= " + COLUMN_LOW_STOCK_ALERT + " AND " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // --- Backup & Restore Methods ---

    public List<Product> getAllProductsList() {
        List<Product> list = new ArrayList<>();
        Cursor cursor = getAllProducts();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(mapCursorToProduct(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Map<String, Object>> getTransactionsForBackup() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> map = new HashMap<>();
                map.put(COLUMN_TRX_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRX_ID)));
                map.put(COLUMN_TRX_TIMESTAMP, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRX_TIMESTAMP)));
                map.put(COLUMN_TRX_TOTAL_AMOUNT, cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRX_TOTAL_AMOUNT)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Map<String, Object>> getTransactionDetailsForBackup() {
        List<Map<String, Object>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTION_DETAILS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
        if (cursor.moveToFirst()) {
            do {
                Map<String, Object> map = new HashMap<>();
                map.put(COLUMN_DETAIL_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_ID)));
                map.put(COLUMN_DETAIL_TRX_ID, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_TRX_ID)));
                map.put(COLUMN_DETAIL_PRODUCT_BARCODE, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRODUCT_BARCODE)));
                map.put(COLUMN_DETAIL_PRODUCT_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRODUCT_NAME)));
                map.put(COLUMN_DETAIL_QUANTITY_SOLD, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_QUANTITY_SOLD)));
                map.put(COLUMN_DETAIL_PRICE_AT_SALE, cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRICE_AT_SALE)));
                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void upsertProduct(Product product, boolean shouldLog) {
        if (product.getBarcode() != null && !product.getBarcode().isEmpty() && productExists(product.getBarcode())) {
            updateProductByBarcode(product, shouldLog);
        } else {
            addProduct(product, shouldLog);
        }
    }

    public void updateProductByBarcode(Product product) {
        updateProductByBarcode(product, true);
    }

    // 🔥 CHANGED: Return boolean to fix compilation error
    public boolean updateProductByBarcode(Product product, boolean shouldLog) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_CATEGORY, product.getCategory());
        values.put(COLUMN_SELLING_PRICE, product.getSellingPrice());
        values.put(COLUMN_COST_PRICE, product.getCostPrice());
        values.put(COLUMN_STOCK_QTY, product.getStockQuantity());
        values.put(COLUMN_LOW_STOCK_ALERT, product.getLowStockAlert());
        values.put(COLUMN_WEIGHT, product.getWeight());
        values.put(COLUMN_EXPIRATION_DATE, product.getExpirationDate());

        int rows = db.update(TABLE_PRODUCTS, values,
                COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{product.getBarcode(), getCurrentUserId()});

        if (rows > 0 && shouldLog) {
            logActivity("EDIT", "Updated: " + product.getName());
        }
        return rows > 0;
    }

    public void restoreTransaction(Map<String, Object> data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, getCurrentUserId());
        values.put(COLUMN_TRX_ID, (Long) data.get(COLUMN_TRX_ID));
        values.put(COLUMN_TRX_TIMESTAMP, (String) data.get(COLUMN_TRX_TIMESTAMP));
        values.put(COLUMN_TRX_TOTAL_AMOUNT, Double.parseDouble(String.valueOf(data.get(COLUMN_TRX_TOTAL_AMOUNT))));
        db.insertWithOnConflict(TABLE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void restoreTransactionDetail(Map<String, Object> data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, getCurrentUserId());
        values.put(COLUMN_DETAIL_ID, (Long) data.get(COLUMN_DETAIL_ID));
        values.put(COLUMN_DETAIL_TRX_ID, (Long) data.get(COLUMN_DETAIL_TRX_ID));
        values.put(COLUMN_DETAIL_PRODUCT_BARCODE, (String) data.get(COLUMN_DETAIL_PRODUCT_BARCODE));
        values.put(COLUMN_DETAIL_PRODUCT_NAME, (String) data.get(COLUMN_DETAIL_PRODUCT_NAME));
        values.put(COLUMN_DETAIL_QUANTITY_SOLD, Integer.parseInt(String.valueOf(data.get(COLUMN_DETAIL_QUANTITY_SOLD))));
        values.put(COLUMN_DETAIL_PRICE_AT_SALE, Double.parseDouble(String.valueOf(data.get(COLUMN_DETAIL_PRICE_AT_SALE))));
        db.insertWithOnConflict(TABLE_TRANSACTION_DETAILS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void beginTransaction() {
        getWritableDatabase().beginTransaction();
    }

    public void setTransactionSuccessful() {
        getWritableDatabase().setTransactionSuccessful();
    }

    public void endTransaction() {
        getWritableDatabase().endTransaction();
    }
}