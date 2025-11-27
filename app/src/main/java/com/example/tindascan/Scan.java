package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class Scan extends Fragment {
    public Scan() {
        super(R.layout.scan);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Scanner");
        }

        // 1. Find the MaterialCardView elements
        MaterialCardView cardPriceCheck = view.findViewById(R.id.card_price_check);
        MaterialCardView cardAddToCart = view.findViewById(R.id.card_add_to_cart);
        MaterialCardView cardInventoryCheck = view.findViewById(R.id.card_inventory_check);

        // 2. Implement Click Listeners for Navigation

        // Price Check: Navigate using the defined ACTION ID.
        cardPriceCheck.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_scan_to_price_check);
        });

        // Add to Cart: Navigate using the defined ACTION ID.
        cardAddToCart.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_scan_to_cart);
        });

        // Inventory Check: Navigate using the defined ACTION ID.
        cardInventoryCheck.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_scan_to_inventory_management);
        });
    }
}