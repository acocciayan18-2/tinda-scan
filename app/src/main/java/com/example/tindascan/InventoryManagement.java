package com.example.tindascan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Fragment for Inventory Management.
 * This screen would typically contain a list/recycler view to display all products
 * and buttons to add/edit inventory.
 */
public class InventoryManagement extends Fragment {

    public InventoryManagement() {
        super(R.layout.fragment_inventory);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Optional: Set the Action Bar Title
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Inventory");
        }

        // You would initialize your list/recycler view, connect to DatabaseHelper,
        // and handle inventory CRUD (Create, Read, Update, Delete) operations here.
    }
}