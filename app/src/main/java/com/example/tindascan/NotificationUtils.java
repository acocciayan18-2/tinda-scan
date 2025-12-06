package com.example.tindascan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class NotificationUtils {

    private Context context;
    private static final String PREFS_NAME = "TindaScanSettings";

    // Channel IDs
    public static final String CHANNEL_INVENTORY = "inventory_channel";
    public static final String CHANNEL_EXPIRY = "expiry_channel";
    public static final String CHANNEL_REPORTS = "reports_channel";
    public static final String CHANNEL_SYSTEM = "system_channel";

    public NotificationUtils(Context context) {
        this.context = context;
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // 1. Inventory Channel
            NotificationChannel inventory = new NotificationChannel(CHANNEL_INVENTORY, "Inventory Alerts", NotificationManager.IMPORTANCE_HIGH);
            inventory.setDescription("Low stock and out of stock alerts");

            // 2. Expiry Channel
            NotificationChannel expiry = new NotificationChannel(CHANNEL_EXPIRY, "Expiration Warnings", NotificationManager.IMPORTANCE_DEFAULT);
            expiry.setDescription("Products nearing expiration date");

            // 3. Reports Channel
            NotificationChannel reports = new NotificationChannel(CHANNEL_REPORTS, "Daily Reports", NotificationManager.IMPORTANCE_LOW);
            reports.setDescription("End of day sales summary");

            // 4. System Channel
            NotificationChannel system = new NotificationChannel(CHANNEL_SYSTEM, "System & Backup", NotificationManager.IMPORTANCE_MIN);
            system.setDescription("Backup reminders");

            if (manager != null) {
                manager.createNotificationChannel(inventory);
                manager.createNotificationChannel(expiry);
                manager.createNotificationChannel(reports);
                manager.createNotificationChannel(system);
            }
        }
    }


    public void checkInventoryNotifications(DatabaseHelper db) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notify_inventory", true)) return;

        List<Product> lowStock = db.getLowStockProducts();
        List<Product> outOfStock = db.getOutOfStockProducts();

        for (Product p : outOfStock) {
            db.saveNotification("Out of Stock",
                    p.getName() + " is sold out. Restock immediately.", "INVENTORY");
        }
        for (Product p : lowStock) {
            db.saveNotification("Low Stock Alert",
                    p.getName() + " is running low (" + p.getStockQuantity() + " left).", "INVENTORY");
        }

        if (!outOfStock.isEmpty()) {
            String message = outOfStock.size() + " items are sold out. Check app for details.";
            showSystemNotification(CHANNEL_INVENTORY, 101, "Stock Alert", message);
        } else if (!lowStock.isEmpty()) {
            String message = lowStock.size() + " items are running low.";
            showSystemNotification(CHANNEL_INVENTORY, 102, "Inventory Warning", message);
        }
    }

    public void checkExpiryNotifications(DatabaseHelper db) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notify_expiry", true)) return;

        List<Product> expiring = db.getExpiringProducts();

        for (Product p : expiring) {
            db.saveNotification("Expiring Soon",
                    p.getName() + " expires on " + p.getExpirationDate() + ".", "EXPIRY");
        }

        if (!expiring.isEmpty()) {
            showSystemNotification(CHANNEL_EXPIRY, 201, "Expiration Warning",
                    expiring.size() + " products are expiring within 7 days.");
        }
    }

    public void checkBackupReminder(DatabaseHelper dbHelper) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notify_system", true)) return;

        long lastBackup = prefs.getLong("last_backup_timestamp", 0);
        long threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L);

        if (lastBackup < threeDaysAgo) {
            String title = "Backup Reminder";
            String msg = "You haven't backed up your data recently. Sync now to prevent data loss.";

            dbHelper.saveNotification(title, msg, "SYSTEM");

            showSystemNotification(CHANNEL_SYSTEM, 301, title, msg);
        }
    }

    public void showDailyReport(double sales, int transactionCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notify_reports", true)) return;

        if (sales > 0) {
            String title = "Daily Sales Report";
            String msg = String.format("Good job! You made ₱%.2f from %d transactions today.", sales, transactionCount);

            DatabaseHelper db = new DatabaseHelper(context);
            db.saveNotification(title, msg, "REPORT");

            showSystemNotification(CHANNEL_REPORTS, 401, title, msg);
        }
    }

    private void showSystemNotification(String channelId, int notificationId, String title, String content) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.tinda_scan) // Ensure this drawable exists
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateLastBackupTimestamp() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply();
    }
}