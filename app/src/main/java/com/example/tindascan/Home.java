package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Home extends Fragment {

    private TextView tvDate, tvProfitValue, tvSalesValue, tvProductsValue, tvLowStockValue;
    private ImageView ivRefresh;
    private DatabaseHelper dbHelper;

    public Home() {
        super(R.layout.home); // Loads the layout defined in 'home.xml'
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Set Action Bar Title
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Home");
        }

        // 2. Initialize Database Helper
        dbHelper = new DatabaseHelper(requireContext());

        // 3. Initialize Views
        tvDate = view.findViewById(R.id.tv_date);
        tvProfitValue = view.findViewById(R.id.tv_profit_value); // Ensure this ID exists in XML
        tvSalesValue = view.findViewById(R.id.tv_sales_value);   // Ensure this ID exists in XML
        tvProductsValue = view.findViewById(R.id.tv_products_value);
        tvLowStockValue = view.findViewById(R.id.tv_low_stock_value);
//        ivRefresh = view.findViewById(R.id.iv_refresh);

        // 4. Set Today's Date
        String currentDate = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        if (tvDate != null) {
            tvDate.setText(currentDate);
        }

        // 5. Set Refresh Listener
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> {
                loadDashboardStats();
                Toast.makeText(getContext(), "Dashboard updated", Toast.LENGTH_SHORT).show();
            });
        }

        // Initial Load
        loadDashboardStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Automatically refresh stats when returning to the Home screen
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        if (dbHelper == null) return;

        // 1. Get Profit
        double profit = dbHelper.getTodayProfit();
        if (tvProfitValue != null) {
            // Display without decimals (%.0f rounds to nearest integer)
            tvProfitValue.setText("₱" + String.format(Locale.US, "%.0f", profit));
        }

        // 2. Get Today's Sales (Replaces Orders)
        double sales = dbHelper.getTodaySalesTotal();
        if (tvSalesValue != null) {
            // Display without decimals (%.0f rounds to nearest integer)
            tvSalesValue.setText("₱" + String.format(Locale.US, "%.0f", sales));
        }

        // 3. Get Unique Product Count
        int productCount = dbHelper.getTotalUniqueProducts();
        if (tvProductsValue != null) {
            tvProductsValue.setText(String.valueOf(productCount));
        }

        // 4. Get Low Stock Count
        int lowStockCount = dbHelper.getLowStockCount();
        if (tvLowStockValue != null) {
            tvLowStockValue.setText(String.valueOf(lowStockCount));
            // Removed color changing logic to keep default text color
        }
    }
}