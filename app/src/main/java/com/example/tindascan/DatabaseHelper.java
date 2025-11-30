package com.example.tindascan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tindascan.db";
    private static final int DATABASE_VERSION = 4; // Version 4 adds user_id support

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

    // --- Transaction Tables ---
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_TRX_ID = "transaction_id";
    private static final String COLUMN_TRX_TIMESTAMP = "timestamp";
    private static final String COLUMN_TRX_TOTAL_AMOUNT = "total_amount";

    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_TRX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " TEXT NOT NULL, " +
                    COLUMN_TRX_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    COLUMN_TRX_TOTAL_AMOUNT + " REAL NOT NULL" +
                    ");";

    private static final String TABLE_TRANSACTION_DETAILS = "transaction_details";
    private static final String COLUMN_DETAIL_ID = "detail_id";
    private static final String COLUMN_DETAIL_TRX_ID = "transaction_id";
    private static final String COLUMN_DETAIL_PRODUCT_BARCODE = "product_barcode";
    private static final String COLUMN_DETAIL_PRODUCT_NAME = "product_name_at_sale";
    private static final String COLUMN_DETAIL_QUANTITY_SOLD = "quantity_sold";
    private static final String COLUMN_DETAIL_PRICE_AT_SALE = "price_at_sale";

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PRODUCTS);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_TRANSACTION_DETAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION_DETAILS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    // 🔥 Helper to get current User ID securely
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return "offline_user";
    }

    // ==========================================
    // PRODUCT METHODS (FILTERED BY USER)
    // ==========================================

    public boolean addProduct(Product product) {
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
        db.close();
        return result != -1;
    }

    public Cursor getAllProducts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{getCurrentUserId()});
    }

    public boolean updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_CATEGORY, product.getCategory());
        values.put(COLUMN_BARCODE, product.getBarcode());
        values.put(COLUMN_SELLING_PRICE, product.getSellingPrice());
        values.put(COLUMN_COST_PRICE, product.getCostPrice());
        values.put(COLUMN_STOCK_QTY, product.getStockQuantity());
        values.put(COLUMN_LOW_STOCK_ALERT, product.getLowStockAlert());
        values.put(COLUMN_WEIGHT, product.getWeight());
        values.put(COLUMN_EXPIRATION_DATE, product.getExpirationDate());

        int rowsAffected = db.update(TABLE_PRODUCTS, values,
                COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(product.getId()), getCurrentUserId()});
        db.close();
        return rowsAffected > 0;
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
        db.close();
        return rows > 0;
    }

    // 🔥 Added missing method for Scan.java
    public boolean deleteProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS,
                COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{barcode.trim(), getCurrentUserId()});
        db.close();
        return rows > 0;
    }

    public boolean productExists(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{barcode, getCurrentUserId()});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public Product getProductByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{barcode, getCurrentUserId()});

        if (cursor.moveToFirst()) {
            Product product = new Product(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SELLING_PRICE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STOCK_QTY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE))
            );
            cursor.close();
            return product;
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
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE));
                double sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SELLING_PRICE));
                int stockQty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STOCK_QTY));
                String weight = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
                String expirationDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE));

                double costPrice = 0.0;
                if(cursor.getColumnIndex(COLUMN_COST_PRICE) != -1)
                    costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE));
                int lowStockAlert = 5;
                if(cursor.getColumnIndex(COLUMN_LOW_STOCK_ALERT) != -1)
                    lowStockAlert = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOW_STOCK_ALERT));

                productList.add(new Product(id, name, category, barcode, sellingPrice,
                        stockQty, weight, expirationDate, costPrice, lowStockAlert));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return productList;
    }

    // ==========================================
    // TRANSACTION METHODS (FILTERED)
    // ==========================================

    public boolean recordSale(List<CartItem> cartItems, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        String userId = getCurrentUserId();

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
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
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

    // 🔥 Added missing method for History.java
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

    // 🔥 Added missing method for History Details
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

    // ==========================================
    // BACKUP & RESTORE METHODS (FILTERED)
    // ==========================================

    public List<Product> getAllProductsList() {
        List<Product> list = new ArrayList<>();
        Cursor cursor = getAllProducts();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE));
                double sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SELLING_PRICE));
                int stockQty = cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity"));
                String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
                String expirationDate = cursor.getString(cursor.getColumnIndexOrThrow("expiration_date"));

                double costPrice = 0.0;
                if(cursor.getColumnIndex("cost_price") != -1) costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
                int lowStockAlert = 5;
                if(cursor.getColumnIndex("low_stock_alert") != -1) lowStockAlert = cursor.getInt(cursor.getColumnIndexOrThrow("low_stock_alert"));

                list.add(new Product(id, name, category, barcode, sellingPrice, stockQty, weight, expirationDate, costPrice, lowStockAlert));
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

    public void upsertProduct(Product product) {
        if (product.getBarcode() != null && !product.getBarcode().isEmpty() && productExists(product.getBarcode())) {
            updateProductByBarcode(product);
        } else {
            addProduct(product);
        }
    }

    public void updateProductByBarcode(Product product) {
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

        db.update(TABLE_PRODUCTS, values,
                COLUMN_BARCODE + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{product.getBarcode(), getCurrentUserId()});
        db.close();
    }

    public void restoreTransaction(Map<String, Object> data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, getCurrentUserId());
        values.put(COLUMN_TRX_ID, (Long) data.get(COLUMN_TRX_ID));
        values.put(COLUMN_TRX_TIMESTAMP, (String) data.get(COLUMN_TRX_TIMESTAMP));
        values.put(COLUMN_TRX_TOTAL_AMOUNT, Double.parseDouble(String.valueOf(data.get(COLUMN_TRX_TOTAL_AMOUNT))));

        db.insertWithOnConflict(TABLE_TRANSACTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
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
        db.close();
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
}