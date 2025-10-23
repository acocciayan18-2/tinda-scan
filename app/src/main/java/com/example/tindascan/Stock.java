package com.example.tindascan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class Stock extends Fragment implements AddProductDialog.OnProductAddedListener {

    private MaterialButton addProductButton;

    public Stock() {
        super(R.layout.stock); // Use your stock layout (res/layout/stock.xml)
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set toolbar title to “Stock”
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Stock");
        }

        // Initialize button
        addProductButton = view.findViewById(R.id.btn_add_product);

        // Open dialog when button is clicked
        addProductButton.setOnClickListener(v -> showAddProductDialog());
    }

    private void showAddProductDialog() {
        AddProductDialog dialog = new AddProductDialog();
        dialog.setOnProductAddedListener(this);
        dialog.show(getParentFragmentManager(), "AddProductDialog");
    }

    @Override
    public void onProductAdded(Product newProduct) {
        Log.d("StockFragment", "New Product Received: " + newProduct.getName());
        Toast.makeText(getContext(), "Product '" + newProduct.getName() + "' added!", Toast.LENGTH_SHORT).show();
        // TODO: Add to RecyclerView or save to database here
    }
}
