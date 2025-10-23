package com.example.tindascan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddProductDialog extends DialogFragment {

    private TextInputEditText etProductName, etBarcode, etSellingPrice,
            etCostPrice, etStockQuantity, etLowStockAlert, etExpirationDate;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnCancel, btnAddProduct;
    private ImageButton btnClose;
    private TextInputLayout tilExpirationDate;

    // Listener to pass data back to parent fragment
    public interface OnProductAddedListener {
        void onProductAdded(Product newProduct);
    }

    private OnProductAddedListener listener;

    public void setOnProductAddedListener(OnProductAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_add_product, container, false);

        // Initialize views
        etProductName = view.findViewById(R.id.et_product_name);
        etBarcode = view.findViewById(R.id.et_barcode);
        actvCategory = view.findViewById(R.id.actv_category);
        etSellingPrice = view.findViewById(R.id.et_selling_price);
        etCostPrice = view.findViewById(R.id.et_cost_price);
        etStockQuantity = view.findViewById(R.id.et_stock_quantity);
        etLowStockAlert = view.findViewById(R.id.et_low_stock_alert);
        etExpirationDate = view.findViewById(R.id.et_expiration_date);
        tilExpirationDate = view.findViewById(R.id.til_expiration_date);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnAddProduct = view.findViewById(R.id.btn_add_product);
        btnClose = view.findViewById(R.id.btn_close_dialog);

        // Setup category dropdown
        String[] categories = {"Electronics", "Groceries", "Apparel", "Books", "Home Goods", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);

        // Setup date picker
        setupDatePicker(view);

        // Button actions
        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        btnAddProduct.setOnClickListener(v -> handleAddProduct());

        return view;
    }

    private void setupDatePicker(View view) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = (picker, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            String format = "MM/dd/yy";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            etExpirationDate.setText(sdf.format(calendar.getTime()));
        };

        // When clicking on the EditText itself
        etExpirationDate.setOnClickListener(v -> showDatePicker(calendar, dateSetListener));

        // When clicking on the calendar icon (end icon)
        tilExpirationDate.setEndIconOnClickListener(v -> showDatePicker(calendar, dateSetListener));
    }

    private void showDatePicker(Calendar calendar, DatePickerDialog.OnDateSetListener listener) {
        new DatePickerDialog(requireContext(), listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }



    private void handleAddProduct() {
        String name = etProductName.getText().toString().trim();
        String barcode = etBarcode.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String sellingPriceStr = etSellingPrice.getText().toString().trim();
        String costPriceStr = etCostPrice.getText().toString().trim();
        String stockQuantityStr = etStockQuantity.getText().toString().trim();
        String lowStockAlertStr = etLowStockAlert.getText().toString().trim();
        String expirationDate = etExpirationDate.getText().toString().trim();

        if (name.isEmpty() || barcode.isEmpty() || category.isEmpty() ||
                sellingPriceStr.isEmpty() || stockQuantityStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        double sellingPrice = Double.parseDouble(sellingPriceStr);
        double costPrice = costPriceStr.isEmpty() ? 0.0 : Double.parseDouble(costPriceStr);
        int stockQuantity = Integer.parseInt(stockQuantityStr);
        int lowStockAlert = lowStockAlertStr.isEmpty() ? 0 : Integer.parseInt(lowStockAlertStr);

        // Example Product object (replace with your real class)
        // Product newProduct = new Product(name, barcode, category, sellingPrice, costPrice, stockQuantity, lowStockAlert, expirationDate);

        if (listener != null) {
            // listener.onProductAdded(newProduct);
            Toast.makeText(getContext(), "Product Added: " + name, Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}
