package com.example.tindascan;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Stock extends Fragment implements AddProductFragment.OnProductAddedListener {

    private MaterialButton addProductButton;
    private RecyclerView recyclerView;
    private StockAdapter adapter;
    private List<Product> productList;
    private DatabaseHelper dbHelper;
    private SearchView searchView;
    private TextView tvProductCount;

    // 🔥 NEW: Empty State TextView
    private TextView tvEmptyState;

    private AutoCompleteTextView dropdownCategories, dropdownStock;

    // Saved filter state
    private String lastSelectedCategory = "All Categories";
    private String lastSelectedStock = "All Stock";
    private String lastSearchQuery = "";

    private final String[] categories = {
            "All Categories",
            "Food Staples",
            "Canned Goods",
            "Instant Foods",
            "Snacks & Candies",
            "Beverages",
            "Toiletries",
            "Household Cleaning",
            "Medicine",
            "Other"
    };

    private final String[] stockFilters = {
            "All Stock",
            "Expired",
            "Expiring Soon"
    };

    private ArrayAdapter<String> categoriesAdapter;
    private ArrayAdapter<String> stockAdapter;

    public Stock() {
        super(R.layout.stock);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        recyclerView = view.findViewById(R.id.rv_stock_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize Views
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvProductCount = view.findViewById(R.id.tv_product_count);

        productList = new ArrayList<>();
        adapter = new StockAdapter(requireContext(), productList, new StockAdapter.OnProductActionListener() {
            @Override
            public void onDelete(Product product) {
                confirmAndDeleteProduct(product);
            }

            @Override
            public void onEdit(Product product) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("selected_product", product);
                NavHostFragment.findNavController(Stock.this)
                        .navigate(R.id.nav_edit_product, bundle);
            }
        });
        recyclerView.setAdapter(adapter);

        dropdownCategories = view.findViewById(R.id.dropdown_categories);
        dropdownStock = view.findViewById(R.id.dropdown_stock);

        if (categoriesAdapter == null) {
            categoriesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        }
        dropdownCategories.setAdapter(categoriesAdapter);

        if (stockAdapter == null) {
            stockAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, stockFilters);
        }
        dropdownStock.setAdapter(stockAdapter);

        // Dropdown listeners
        dropdownCategories.setOnItemClickListener((parent, view1, position, id) -> {
            lastSelectedCategory = categories[position];
            applyFilters();
        });

        dropdownStock.setOnItemClickListener((parent, view1, position, id) -> {
            lastSelectedStock = stockFilters[position];
            applyFilters();
        });

        // SearchView listener
        searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { searchView.clearFocus(); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { lastSearchQuery = newText; applyFilters(); return true; }
        });

        addProductButton = view.findViewById(R.id.btn_add_product);
        addProductButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_add_product)
        );

        loadProductsFromDatabase();
    }


    private void loadProductsFromDatabase() {
        productList.clear();
        Cursor cursor = dbHelper.getAllProducts();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode"));
                double sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("selling_price"));
                int stockQty = cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity"));
                String weight = cursor.getString(cursor.getColumnIndexOrThrow("weight"));
                String expirationDate = cursor.getString(cursor.getColumnIndexOrThrow("expiration_date"));

                // Get missing columns safely
                double costPrice = 0.0;
                if(cursor.getColumnIndex("cost_price") != -1)
                    costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));

                int lowStockAlert = 5;
                if(cursor.getColumnIndex("low_stock_alert") != -1)
                    lowStockAlert = cursor.getInt(cursor.getColumnIndexOrThrow("low_stock_alert"));

                productList.add(new Product(id, name, category, barcode, sellingPrice, stockQty, weight, expirationDate, costPrice, lowStockAlert));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter.setInitialProductList(productList);

        // Restore UI state
        dropdownCategories.setText(lastSelectedCategory, false);
        dropdownStock.setText(lastSelectedStock, false);
        searchView.setQuery(lastSearchQuery, false);

        applyFilters();
    }

    private void applyFilters() {
        List<Product> filteredList = new ArrayList<>();

        // Prepare stock dates
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long todayMillis = cal.getTimeInMillis();
        long expiringThresholdMillis = todayMillis + (7L * 24 * 60 * 60 * 1000);

        for (Product p : productList) {
            boolean matchesCategory = lastSelectedCategory.equals("All Categories") || p.getCategory().equals(lastSelectedCategory);

            long expMillis = p.getExpirationDateMillis();
            boolean hasExpiration = p.getExpirationDate() != null && !p.getExpirationDate().isEmpty();
            boolean matchesStock;

            switch (lastSelectedStock) {
                case "Expired":
                    matchesStock = hasExpiration && expMillis < todayMillis;
                    break;
                case "Expiring Soon":
                    matchesStock = hasExpiration && expMillis >= todayMillis && expMillis <= expiringThresholdMillis;
                    break;
                default:
                    matchesStock = true;
                    break;
            }

            boolean matchesSearch = lastSearchQuery.isEmpty() ||
                    p.getName().toLowerCase().contains(lastSearchQuery.toLowerCase()) ||
                    (p.getBarcode() != null && p.getBarcode().contains(lastSearchQuery));

            if (matchesCategory && matchesStock && matchesSearch) {
                filteredList.add(p);
            }
        }

        adapter.updateList(filteredList);

        // Update product count header
        if (tvProductCount != null) {
            tvProductCount.setText(filteredList.size() + " products");
        }

        // 🔥 NEW: Toggle Empty State Visibility
        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);

            if (!lastSelectedStock.equals("All Stock")) {
                tvEmptyState.setText("No " + lastSelectedStock.toLowerCase() + " products found.");
            } else if (!lastSearchQuery.isEmpty()) {
                tvEmptyState.setText("No results found for \"" + lastSearchQuery + "\"");
            } else {
                tvEmptyState.setText("No products found matching your filters.");
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void confirmAndDeleteProduct(Product product) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + product.getName() + "\" from stock?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (dbHelper.deleteProduct(product)) {
                        Toast.makeText(requireContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                        loadProductsFromDatabase();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete product", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onProductAdded(Product newProduct) {
        loadProductsFromDatabase();
    }

    @Override
    public void onResume() {
        super.onResume();

        categoriesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        dropdownCategories.setAdapter(categoriesAdapter);

        stockAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, stockFilters);
        dropdownStock.setAdapter(stockAdapter);

        // Sync variables with the text shown on screen
        String currentCategoryText = dropdownCategories.getText().toString();
        String currentStockText = dropdownStock.getText().toString();

        if (!currentCategoryText.isEmpty()) lastSelectedCategory = currentCategoryText;
        if (!currentStockText.isEmpty()) lastSelectedStock = currentStockText;

        applyFilters();
    }
}