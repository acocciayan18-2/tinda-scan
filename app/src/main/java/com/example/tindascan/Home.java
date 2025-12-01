package com.example.tindascan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends Fragment {

    private TextView tvDate, tvProfitValue, tvSalesValue, tvProductsValue, tvLowStockValue, tvNoActivities;
    private ImageView ivRefresh;
    private ImageButton btnClearActivities;
    private RecyclerView rvRecentActivities;
    private RecentActivityAdapter activityAdapter;
    private DatabaseHelper dbHelper;

    // 🔥 Notification Views
    private View notifBell;
    private TextView tvBadge;

    public Home() {
        super(R.layout.home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Home");
        }

        dbHelper = new DatabaseHelper(requireContext());

        // Initialize Views
        tvDate = view.findViewById(R.id.tv_date);
        tvProfitValue = view.findViewById(R.id.tv_profit_value);
        tvSalesValue = view.findViewById(R.id.tv_sales_value);
        tvProductsValue = view.findViewById(R.id.tv_products_value);
        tvLowStockValue = view.findViewById(R.id.tv_low_stock_value);
        ivRefresh = view.findViewById(R.id.iv_refresh); // Re-enabled refresh
        tvNoActivities = view.findViewById(R.id.tv_no_activities);

        // 🔥 Initialize Notification Views
        notifBell = view.findViewById(R.id.fl_notification);
        tvBadge = view.findViewById(R.id.tv_notification_badge);

        // Activity List Setup
        rvRecentActivities = view.findViewById(R.id.rv_recent_activities);
        rvRecentActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        // Disable nested scrolling so the main page scrolls, not just the list
        rvRecentActivities.setNestedScrollingEnabled(false);

        activityAdapter = new RecentActivityAdapter(requireContext(), new ArrayList<>());
        rvRecentActivities.setAdapter(activityAdapter);

        // Clear Button Setup
        btnClearActivities = view.findViewById(R.id.btn_clear_activities);
        if (btnClearActivities != null) {
            btnClearActivities.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Clear Activities")
                        .setMessage("Are you sure you want to delete all recent activity logs?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dbHelper.clearAllActivities();
                            loadDashboardStats(); // Refresh
                            Toast.makeText(getContext(), "Logs cleared", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        // 🔥 Notification Bell Click Listener
        if (notifBell != null) {
            notifBell.setOnClickListener(v -> {
                // Navigate to Notifications Fragment
                NavHostFragment.findNavController(this).navigate(R.id.nav_notifications);
            });
        }

        // Set Date
        String currentDate = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        if (tvDate != null) tvDate.setText(currentDate);

        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> {
                loadDashboardStats();
                Toast.makeText(getContext(), "Dashboard updated", Toast.LENGTH_SHORT).show();
            });
        }

        loadDashboardStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        if (dbHelper == null) return;

        // 1. Load Statistics
        double profit = dbHelper.getTodayProfit();
        if (tvProfitValue != null) tvProfitValue.setText("₱" + String.format(Locale.US, "%.0f", profit));

        double sales = dbHelper.getTodaySalesTotal();
        if (tvSalesValue != null) tvSalesValue.setText("₱" + String.format(Locale.US, "%.0f", sales));

        int productCount = dbHelper.getTotalUniqueProducts();
        if (tvProductsValue != null) tvProductsValue.setText(String.valueOf(productCount));

        int lowStockCount = dbHelper.getLowStockCount();
        if (tvLowStockValue != null) tvLowStockValue.setText(String.valueOf(lowStockCount));

        // 2. Load Recent Activities
        List<ActivityLog> logs = dbHelper.getRecentActivities();
        if (activityAdapter != null) {
            activityAdapter.updateList(logs);
        }

        if (logs.isEmpty()) {
            if (tvNoActivities != null) tvNoActivities.setVisibility(View.VISIBLE);
            if (rvRecentActivities != null) rvRecentActivities.setVisibility(View.GONE);
        } else {
            if (tvNoActivities != null) tvNoActivities.setVisibility(View.GONE);
            if (rvRecentActivities != null) rvRecentActivities.setVisibility(View.VISIBLE);
        }

        // 🔥 3. Update Notification Badge
        if (tvBadge != null) {
            int unreadCount = dbHelper.getUnreadNotificationCount();
            if (unreadCount > 0) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(String.valueOf(unreadCount));
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }
    }
}