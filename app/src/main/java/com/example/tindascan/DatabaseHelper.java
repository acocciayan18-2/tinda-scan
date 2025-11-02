package com.example.tindascan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tindascan.db";
    private static final int DATABASE_VERSION = 3;

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
                    COLUMN_DETAIL_TRX_ID + " INTEGER NOT NULL, " +
                    COLUMN_DETAIL_PRODUCT_BARCODE + " TEXT, " +
                    COLUMN_DETAIL_PRODUCT_NAME + " TEXT NOT NULL, " +
                    COLUMN_DETAIL_QUANTITY_SOLD + " INTEGER NOT NULL, " +
                    COLUMN_DETAIL_PRICE_AT_SALE + " REAL NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_DETAIL_TRX_ID + ") REFERENCES " + TABLE_TRANSACTIONS + "(" + COLUMN_TRX_ID + ")" +
                    ");";
    // --- End Transaction Tables ---


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

    public boolean insertProduct(String name, String weight, String barcode, String category,
                                 double sellingPrice, double costPrice,
                                 int stockQty, String expirationDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_BARCODE, barcode);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_SELLING_PRICE, sellingPrice);
        values.put(COLUMN_COST_PRICE, costPrice);
        values.put(COLUMN_STOCK_QTY, stockQty);
        values.put(COLUMN_EXPIRATION_DATE, expirationDate);

        long result = db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();

        return result != -1;
    }


    /**
     * Records a sale transaction and deducts stock, preserving the product record
     * even when stock reaches zero.
     */
    public boolean recordSale(List<CartItem> cartItems, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            ContentValues trxValues = new ContentValues();
            trxValues.put(COLUMN_TRX_TOTAL_AMOUNT, totalAmount);
            long transactionId = db.insert(TABLE_TRANSACTIONS, null, trxValues);
            if (transactionId == -1) {
                return false;
            }

            for (CartItem item : cartItems) {
                Product product = item.getProduct();
                if (product == null) continue;

                int quantitySold = item.getQuantity();
                String barcode = product.getBarcode();
                String productName = product.getName();
                double price = product.getPrice();

                // 1. Get the current stock quantity from the database to ensure accuracy
                int currentDbStock = getStockQuantityByBarcode(barcode);
                if (currentDbStock == -1) {
                    Log.w("DatabaseHelper", "Product not found during stock deduction: " + barcode);
                    continue;
                }

                // 2. Calculate the new stock, ensuring it doesn't go negative
                int newStock = currentDbStock - quantitySold;
                if (newStock < 0) {
                    newStock = 0; // Prevent negative stock (PRESERVE RECORD)
                }

                // 3. Insert into transaction_details
                ContentValues detailValues = new ContentValues();
                detailValues.put(COLUMN_DETAIL_TRX_ID, transactionId);
                detailValues.put(COLUMN_DETAIL_PRODUCT_BARCODE, barcode);
                detailValues.put(COLUMN_DETAIL_PRODUCT_NAME, productName);
                detailValues.put(COLUMN_DETAIL_QUANTITY_SOLD, quantitySold);
                detailValues.put(COLUMN_DETAIL_PRICE_AT_SALE, price);
                db.insert(TABLE_TRANSACTION_DETAILS, null, detailValues);

                // 4. Deduct stock (but NEVER delete the product record)
                ContentValues updateValues = new ContentValues();
                updateValues.put(COLUMN_STOCK_QTY, newStock);
                int rowsAffected = db.update(TABLE_PRODUCTS, updateValues, COLUMN_BARCODE + " = ?", new String[]{barcode});

                if (rowsAffected == 0) {
                    Log.w("DatabaseHelper", "Could not find product to update stock: " + barcode);
                }
            }

            db.setTransactionSuccessful();
            return true;

        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error recording sale", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }


    /**
     * Gets the current stock quantity for a product by barcode.
     * Returns -1 if the product is not found.
     */
    public int getStockQuantityByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_STOCK_QTY + " FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=?", new String[]{barcode});
        if (cursor.moveToFirst()) {
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STOCK_QTY));
            cursor.close();
            return stock;
        }
        cursor.close();
        return -1; // Indicates product not found
    }

    /**
     * Retrieves a list of products whose name or barcode contains the search query.
     * Used for manual product lookup in the cart screen.
     */
    public List<Product> getProductsByNameOrBarcode(String query) {
        List<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String search = "%" + query.trim() + "%";

        String selection = COLUMN_NAME + " LIKE ? OR " + COLUMN_BARCODE + " LIKE ?";
        String[] selectionArgs = new String[]{search, search};

        Cursor cursor = db.query(
                TABLE_PRODUCTS,
                null, // columns (null = all)
                selection,
                selectionArgs,
                null, null,
                COLUMN_NAME + " ASC" // Order by name for better display
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Use the 8-argument constructor (Constructor 4 in your Product class)
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
                productList.add(product);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return productList;
    }


    public Cursor getAllProducts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS, null);
    }

    public boolean productExists(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE barcode=?", new String[]{barcode});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Adds a new product to the database.
     */
    public boolean addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
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

    /**
     * Retrieves a Product object by its barcode.
     */
    public Product getProductByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_BARCODE + "=?", new String[]{barcode});
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

    public boolean deleteProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows;
        if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
            rows = db.delete(TABLE_PRODUCTS, COLUMN_BARCODE + " = ?", new String[]{product.getBarcode().trim()});
        } else {
            rows = db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?", new String[]{String.valueOf(product.getId())});
        }
        db.close();
        return rows > 0;
    }

    public boolean deleteProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS, COLUMN_BARCODE + " = ?", new String[]{barcode.trim()});
        db.close();
        return rows > 0;
    }


    /**
     * Gets a list of all transactions for the order history page.
     * NOTE: Requires Transaction class
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COLUMN_TRX_TIMESTAMP + " DESC",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRX_ID));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRX_TIMESTAMP));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRX_TOTAL_AMOUNT));

                // transactionList.add(new Transaction(id, timestamp, total)); // Placeholder
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    /**
     * Gets the specific items for a single transaction.
     * NOTE: Requires TransactionDetail class
     */
    public List<TransactionDetail> getTransactionDetails(long transactionId) {
        List<TransactionDetail> detailList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " +
                COLUMN_DETAIL_PRODUCT_NAME + ", " +
                COLUMN_DETAIL_QUANTITY_SOLD + ", " +
                COLUMN_DETAIL_PRICE_AT_SALE +
                " FROM " + TABLE_TRANSACTION_DETAILS +
                " WHERE " + COLUMN_DETAIL_TRX_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(transactionId)});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRODUCT_NAME));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_QUANTITY_SOLD));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DETAIL_PRICE_AT_SALE));

                // detailList.add(new TransactionDetail(name, qty, price)); // Placeholder
            } while (cursor.moveToNext());
        }
        cursor.close();
        return detailList;
    }
}