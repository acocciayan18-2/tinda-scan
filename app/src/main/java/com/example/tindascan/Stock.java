package com.example.tindascan;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class Stock extends Fragment implements AddProductFragment.OnProductAddedListener {

    private MaterialButton addProductButton;
    private RecyclerView recyclerView;
    private StockAdapter adapter;
    private List<Product> productList;
    private DatabaseHelper dbHelper;

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
        adapter = new StockAdapter(getContext(), productList, new StockAdapter.OnProductActionListener() {
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

        addProductButton = view.findViewById(R.id.btn_add_product);
        addProductButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.nav_add_product);
        });



        loadProductsFromDatabase();
    }

    private void showAddProductDialog() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.nav_add_product);
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

                // ✅ FIX 1: Get the expiration date
                String expirationDate = cursor.getString(cursor.getColumnIndexOrThrow("expiration_date"));

                // ✅ FIX 2: Use the 8-argument constructor from Product.java
                productList.add(new Product(id, name, category, barcode, sellingPrice, stockQty, weight, expirationDate));

            } while (cursor.moveToNext());
            cursor.close();
        }

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
        // This is the correct listener method
        loadProductsFromDatabase();
    }
}