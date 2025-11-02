package com.example.tindascan;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter; // Added import
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class Stock extends Fragment implements AddProductFragment.OnProductAddedListener {

    private MaterialButton addProductButton;
    private RecyclerView recyclerView;
    private StockAdapter adapter;
    private List<Product> productList;
    private DatabaseHelper dbHelper;
    private SearchView searchView;
    private final String[] categories = {
            "All Categories", // Add "All Categories" as the first item for resetting the filter
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
    private AutoCompleteTextView dropdownCategories;

    public Stock() {
        super(R.layout.stock);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Stock");
        }

        dbHelper = new DatabaseHelper(requireContext());
        recyclerView = view.findViewById(R.id.rv_stock_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        productList = new ArrayList<>();
        adapter = new StockAdapter(requireContext(), productList, new StockAdapter.OnProductActionListener() {
            @Override
            public void onDelete(Product product) {
                confirmAndDeleteProduct(product);
            }

            @Override
            public void onEdit(Product product) {
                // future feature
            }
        });

        recyclerView.setAdapter(adapter);

        // --- SEARCH VIEW LOGIC START ---
        searchView = view.findViewById(R.id.search_view);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    // Update: Call applyCategoryFilter to handle compound filtering
                    String selectedCategory = dropdownCategories.getText().toString();
                    applyCategoryFilter(selectedCategory, newText);
                }
                return true;
            }
        });
        // --- SEARCH VIEW LOGIC END ---

        dropdownCategories = view.findViewById(R.id.dropdown_categories);

        // Create an ArrayAdapter using the defined categories array
        ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        dropdownCategories.setAdapter(categoriesAdapter);

        // Set the default text and selection (optional, but good practice)
        dropdownCategories.setText(categories[0], false);

        // Set the item click listener to perform the filtering
        dropdownCategories.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedCategory = categories[position];
            // Update: Pass current query text when changing category
            String currentQuery = searchView.getQuery() != null ? searchView.getQuery().toString() : "";
            applyCategoryFilter(selectedCategory, currentQuery);
        });
        // --- CATEGORY DROPDOWN LOGIC END ---

        addProductButton = view.findViewById(R.id.btn_add_product);
        addProductButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.nav_add_product);
        });

        loadProductsFromDatabase();
    }

    // New method to handle applying the category filter (accepts current query)
    private void applyCategoryFilter(String selectedCategory, String currentQuery) {
        if (adapter != null) {
            // Standardize the filter constraint using a custom token and delimiter
            String filterConstraint;
            if (selectedCategory.equals("All Categories")) {
                // When "All Categories" is selected, only the search query matters
                filterConstraint = "SEARCH_ONLY:" + currentQuery;
            } else {
                // Use the compound filter format: CATEGORY_TOKEN:CategoryName:SearchQuery
                filterConstraint = "COMPOUND:" + selectedCategory + ":" + currentQuery;
            }
            adapter.getFilter().filter(filterConstraint);
        }
    }


    private void showAddProductDialog() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.nav_add_product);
    }


    private void loadProductsFromDatabase() {
        // ... (loading logic remains the same)
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

                productList.add(new Product(id, name, category, barcode, sellingPrice, stockQty, weight, expirationDate));

            } while (cursor.moveToNext());
            cursor.close();
        }

        // Must notify the adapter that the initial list has changed
        adapter.notifyDataSetChanged();
    }

    private void confirmAndDeleteProduct(Product product) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + product.getName() + "\" from stock?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteProduct(product);
                    if (deleted) {
                        Toast.makeText(requireContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                        loadProductsFromDatabase(); // refresh UI
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
}