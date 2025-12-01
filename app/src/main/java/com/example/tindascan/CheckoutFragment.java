package com.example.tindascan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;

// ✅ Make sure you have this import
import com.example.tindascan.CartItem;

import java.util.List;
import java.util.Locale; // ✅ Added for price formatting

public class CheckoutFragment extends Fragment {

    private CartStorage cartStorage;
    private LinearLayout checkoutItemsContainer;

    // ✅ --- ADDED ---
    private DatabaseHelper dbHelper;
    private TextView tvCheckoutTotal;
    private List<CartItem> currentCartItems; // Stores the loaded cart
    // --- END ---

    public CheckoutFragment() {
        super(R.layout.fragment_checkout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cartStorage = new CartStorage(requireContext());
        dbHelper = new DatabaseHelper(requireContext()); // ✅ Initialize dbHelper

        checkoutItemsContainer = view.findViewById(R.id.checkout_items_container);
        // ✅ Make sure this ID exists in your R.layout.fragment_checkout
//        tvCheckoutTotal = view.findViewById(R.id.tv_checkout_total);

        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm_checkout);
        View btnBack = view.findViewById(R.id.btn_back_to_cart);

        // ✅ Load items and update total price
        loadCheckoutItems();

        // ✅ --- UPDATED: This now calls the database ---
        btnConfirm.setOnClickListener(v -> {
            handleConfirmCheckout();
        });

        btnBack.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    /**
     * ✅ NEW: Handles the database logic
     */
    private void handleConfirmCheckout() {
        if (currentCartItems == null || currentCartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Nothing to check out!", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = calculateTotalPrice();

        // Call the database method to record the sale
        boolean success = dbHelper.recordSale(currentCartItems, total);

        if (success) {
            Toast.makeText(requireContext(), "Checkout complete!", Toast.LENGTH_SHORT).show();
            cartStorage.clearCart(); // Clear the SharedPreferences cart
            NavHostFragment.findNavController(this).popBackStack();
        } else {
            Toast.makeText(requireContext(), "Error recording sale. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * ✅ NEW: Helper method to calculate the total price
     */
    private double calculateTotalPrice() {
        double total = 0.0;
        if (currentCartItems == null) return total;

        for (CartItem item : currentCartItems) {
            if (item.getProduct() != null) {
                total += item.getProduct().getSellingPrice() * item.getQuantity();
            }
        }
        return total;
    }

    private void loadCheckoutItems() {
        currentCartItems = cartStorage.loadCart(); // ✅ Save items to a field
        checkoutItemsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        double total = 0.0;

        if (currentCartItems.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No items in cart");
            empty.setPadding(16, 16, 16, 16);
            checkoutItemsContainer.addView(empty);
        } else {
            for (CartItem item : currentCartItems) {
                View itemView = inflater.inflate(R.layout.item_checkout, checkoutItemsContainer, false);

                TextView name = itemView.findViewById(R.id.tv_item_name);
                TextView qty = itemView.findViewById(R.id.tv_item_quantity);
                TextView price = itemView.findViewById(R.id.tv_item_price);

                Product product = item.getProduct();

                if (product != null) {
                    double itemTotal = product.getSellingPrice() * item.getQuantity();
                    total += itemTotal; // Add to the grand total

                    name.setText(product.getName());
                    qty.setText("X" + item.getQuantity());
                    price.setText(String.format(Locale.US, "₱%.2f", itemTotal));
                }
                checkoutItemsContainer.addView(itemView);
            }
        }

        // ✅ Update the total price TextView
        if (tvCheckoutTotal != null) {
            tvCheckoutTotal.setText(String.format(Locale.US, "₱%.2f", total));
        }
    }
}