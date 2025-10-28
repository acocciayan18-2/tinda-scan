package com.example.tindascan;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.database.sqlite.SQLiteConstraintException;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AddProductFragment extends Fragment {

    public static final String REQUEST_KEY_PRODUCT_ADDED = "productAddedRequest";

    private TextInputEditText etProductName, etWeight, etBarcode, etBarcodeInput, etSellingPrice,
            etCostPrice, etStockQuantity, etLowStockAlert, etExpirationDate;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnCancel, btnAddProduct;
    private ImageButton btnClose;

    private TextView tvBarcodeCheckMessage;

    private TextInputLayout tilExpirationDate;
    private PreviewView previewView;
    private FrameLayout cameraContainer;
    private boolean isCameraVisible = false;

    private DatabaseHelper dbHelper;

    public interface OnProductAddedListener {
        void onProductAdded(Product newProduct);
    }

    private OnProductAddedListener productAddedListener;

    public void setOnProductAddedListener(OnProductAddedListener listener) {
        this.productAddedListener = listener;
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_product, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        etProductName = view.findViewById(R.id.et_product_name);
        etWeight = view.findViewById(R.id.et_weight);
        etBarcode = view.findViewById(R.id.et_barcode);
        etBarcodeInput = view.findViewById(R.id.et_barcode_input);
        actvCategory = view.findViewById(R.id.actv_category);
        etSellingPrice = view.findViewById(R.id.et_selling_price);
        etCostPrice = view.findViewById(R.id.et_cost_price);
        etStockQuantity = view.findViewById(R.id.et_stock_quantity);
        etLowStockAlert = view.findViewById(R.id.et_low_stock_alert);
        etExpirationDate = view.findViewById(R.id.et_expiration_date);
        tilExpirationDate = view.findViewById(R.id.til_expiration_date);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnAddProduct = view.findViewById(R.id.btn_add_product);
//        btnClose = view.findViewById(R.id.btn_close_dialog);
        previewView = view.findViewById(R.id.camera_preview);
        cameraContainer = view.findViewById(R.id.camera_container);
        tvBarcodeCheckMessage = view.findViewById(R.id.tv_barcode_check_message);

        cameraContainer.setVisibility(View.GONE);

        // Category dropdown
        String[] categories = {
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);

        setupDatePicker();

        // Replaced dismiss() calls with popBackStack()
        btnCancel.setOnClickListener(v -> goBack());
        btnAddProduct.setOnClickListener(v -> handleAddProduct());

        etBarcode.setOnClickListener(v -> toggleCamera());

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        etBarcodeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show the message if the barcode field is not empty
                if (s != null && s.length() > 0) {
                    tvBarcodeCheckMessage.setVisibility(View.VISIBLE);
                } else {
                    tvBarcodeCheckMessage.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });


        return view;
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), image -> {
                    Image mediaImage = image.getImage();
                    if (mediaImage != null) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                        BarcodeScanner scanner = BarcodeScanning.getClient();
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String value = barcode.getRawValue();
                                        if (value != null && !value.isEmpty()) {
                                            etBarcodeInput.setText(value);
                                            cameraContainer.setVisibility(View.GONE);
                                            isCameraVisible = false;
                                            cameraProvider.unbindAll();
                                            break;
                                        }
                                    }
                                })
                                .addOnCompleteListener(task -> image.close());
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setupDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (picker, year, month, day) -> {
            calendar.set(year, month, day);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.US);
            etExpirationDate.setText(sdf.format(calendar.getTime()));
        };
        etExpirationDate.setOnClickListener(v -> showDatePicker(calendar, dateSetListener));
        tilExpirationDate.setEndIconOnClickListener(v -> showDatePicker(calendar, dateSetListener));
    }

    private void showDatePicker(Calendar calendar, DatePickerDialog.OnDateSetListener listener) {
        new DatePickerDialog(requireContext(), listener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void toggleCamera() {
        isCameraVisible = !isCameraVisible;
        cameraContainer.setVisibility(isCameraVisible ? View.VISIBLE : View.GONE);

        if (isCameraVisible) startCamera();
    }

    // Inside AddProductFragment.java

    private void handleAddProduct() {
        String nameInput = etProductName.getText().toString().trim(); // Get original name
        String weightInput = etWeight.getText().toString().trim(); // Get weight
        String barcode = etBarcodeInput.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String sellingPriceStr = etSellingPrice.getText().toString().trim();
        String costPriceStr = etCostPrice.getText().toString().trim();
        String stockQuantityStr = etStockQuantity.getText().toString().trim();
        String lowStockAlertStr = etLowStockAlert.getText().toString().trim();
        String expirationDate = etExpirationDate.getText().toString().trim();

        // --- ✅ START: Combine Name and Weight ---
        String finalProductName = nameInput; // Default to original name
        if (!weightInput.isEmpty()) {
            // If weight is provided, combine them
            finalProductName = nameInput + " (" + weightInput + ")";
        }
        // --- ✅ END: Combine Name and Weight ---


        if (!barcode.isEmpty() && dbHelper.productExists(barcode)) {
            Toast.makeText(getContext(), "⚠️ Barcode or Product already exists!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use 'finalProductName' for validation check
        if (finalProductName.isEmpty() || category.isEmpty() || sellingPriceStr.isEmpty() || stockQuantityStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        // ... (rest of the parsing logic for numbers remains the same)
        double sellingPrice, costPrice;
        int stockQuantity, lowStockAlert;
        try {
            sellingPrice = Double.parseDouble(sellingPriceStr);
            costPrice = costPriceStr.isEmpty() ? 0.0 : Double.parseDouble(costPriceStr);
            stockQuantity = Integer.parseInt(stockQuantityStr);
            lowStockAlert = lowStockAlertStr.isEmpty() ? 5 : Integer.parseInt(lowStockAlertStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid numeric value.", Toast.LENGTH_SHORT).show();
            return;
        }


        // ✅ Use the 'finalProductName' when creating the Product object
        Product newProduct = new Product(
                finalProductName, // <-- Use the combined name here
                barcode,
                weightInput, // Still save the original weight separately if needed
                category,
                sellingPrice,
                costPrice,
                stockQuantity,
                lowStockAlert,
                expirationDate
        );

        // ... (rest of the saving logic remains the same)
        try {
            boolean inserted = dbHelper.addProduct(newProduct);
            if (inserted) {
                Toast.makeText(getContext(), "✅ Product saved successfully!", Toast.LENGTH_SHORT).show();

                Bundle result = new Bundle();
                result.putBoolean("productAdded", true);
                getParentFragmentManager().setFragmentResult(REQUEST_KEY_PRODUCT_ADDED, result);

                NavHostFragment.findNavController(this).popBackStack();

            } else {
                Toast.makeText(getContext(), "⚠️ Failed to save product.", Toast.LENGTH_SHORT).show();
            }
        } catch (SQLiteConstraintException e) {
            Toast.makeText(getContext(), "⚠️ Barcode already exists!", Toast.LENGTH_SHORT).show();
        }
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }
}
