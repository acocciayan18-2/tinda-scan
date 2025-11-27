package com.example.tindascan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class EditProductFragment extends Fragment {

    private TextInputEditText etName, etBarcode, etWeight, etCost, etPrice, etQty, etLow, etExpiry;
    private AutoCompleteTextView dropdownCategory;
    private MaterialButton btnSave, btnCancel;
    private ImageButton btnBack;
    private TextInputLayout tilExpiration;

    private DatabaseHelper dbHelper;
    private Product currentProduct;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the full-screen layout
        return inflater.inflate(R.layout.fragment_edit_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper(requireContext());

        // 1. Initialize Views
        etName = view.findViewById(R.id.et_edit_name);
        etBarcode = view.findViewById(R.id.et_edit_barcode);
        etWeight = view.findViewById(R.id.et_edit_weight);
        etCost = view.findViewById(R.id.et_edit_cost);
        etPrice = view.findViewById(R.id.et_edit_price);
        etQty = view.findViewById(R.id.et_edit_qty);
        etLow = view.findViewById(R.id.et_edit_low_stock);
        etExpiry = view.findViewById(R.id.et_edit_expiry);
        dropdownCategory = view.findViewById(R.id.dropdown_edit_category);
        btnSave = view.findViewById(R.id.btn_save_changes);
        btnCancel = view.findViewById(R.id.btn_cancel_edit);
        btnBack = view.findViewById(R.id.btn_back_edit);
        tilExpiration = view.findViewById(R.id.til_edit_expiration);

        // 2. Setup Category Dropdown
        String[] categories = {"Food Staples", "Canned Goods", "Instant Foods", "Snacks & Candies", "Beverages", "Toiletries", "Household Cleaning", "Medicine", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        dropdownCategory.setAdapter(adapter);

        // 3. Get Data passed from Stock Fragment
        if (getArguments() != null) {
            currentProduct = (Product) getArguments().getSerializable("selected_product");
            if (currentProduct != null) {
                populateFields();
            }
        }

        // 4. Listeners
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());

        // Date Picker logic
        etExpiry.setOnClickListener(v -> showDatePicker());
        tilExpiration.setEndIconOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                updateProductInDb();
            }
        });
    }

    private void populateFields() {
        etName.setText(currentProduct.getName());
        etBarcode.setText(currentProduct.getBarcode());
        etWeight.setText(currentProduct.getWeight());
        etCost.setText(String.valueOf(currentProduct.getCostPrice()));
        etPrice.setText(String.valueOf(currentProduct.getSellingPrice()));
        etQty.setText(String.valueOf(currentProduct.getStockQuantity()));
        etLow.setText(String.valueOf(currentProduct.getLowStockAlert()));
        etExpiry.setText(currentProduct.getExpirationDate());

        // Set category text without filtering
        dropdownCategory.setText(currentProduct.getCategory(), false);
    }

    private void updateProductInDb() {
        String name = etName.getText().toString().trim();
        String barcode = etBarcode.getText().toString().trim();
        String cat = dropdownCategory.getText().toString();
        String weight = etWeight.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();

        double cost = parseDouble(etCost.getText().toString());
        double price = parseDouble(etPrice.getText().toString());
        int qty = parseInt(etQty.getText().toString());
        int low = parseInt(etLow.getText().toString());

        // Create updated object using the ORIGINAL ID
        Product updatedProduct = new Product(
                currentProduct.getId(),
                name, cat, barcode, price, qty, weight, expiry, cost, low
        );

        if (dbHelper.updateProduct(updatedProduct)) {
            Toast.makeText(requireContext(), "Product updated!", Toast.LENGTH_SHORT).show();
            goBack();
        } else {
            Toast.makeText(requireContext(), "Update failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            String date = year + "-" + (month + 1) + "-" + day; // Simple format
            etExpiry.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validateInputs() {
        if (etName.getText().toString().isEmpty()) { etName.setError("Required"); return false; }
        if (etPrice.getText().toString().isEmpty()) { etPrice.setError("Required"); return false; }
        return true;
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private double parseDouble(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
    private int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
}