package com.example.tindascan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PriceCheckFragment extends Fragment implements ReusableBarcodeAnalyzer.OnBarcodeScannedListener { // <-- IMPLEMENTS REUSABLE ANALYZER

    private static final String TAG = "PriceCheckFragment";

    // UI Elements
    private PreviewView previewView;
    private TextView tvProductName;
    private TextView tvPrice;
    private TextView tvDescription;
    private TextView tvFooterMessage;

    // Scanning Components
    private ExecutorService cameraExecutor;
    private DatabaseHelper dbHelper;
    private final Handler scanCooldownHandler = new Handler(Looper.getMainLooper());
    private static final long SCAN_COOLDOWN_MS = 2000;
    private String lastScannedBarcode = null;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false); // Used by Analyzer

    // --- 1. PERMISSION HANDLING ---
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required for price checking.", Toast.LENGTH_LONG).show();
                }
            });


    public PriceCheckFragment() {
        // Assume R.layout.fragment_price_check is the correct camera/details XML
        super(R.layout.fragment_price_check);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
        dbHelper = new DatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_price_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Price Check");
        }

        // 2. Bind UI elements
        previewView = view.findViewById(R.id.camera_preview);
        tvProductName = view.findViewById(R.id.text_product_name);
        tvPrice = view.findViewById(R.id.text_price);
        tvDescription = view.findViewById(R.id.text_description);

        resetProductUI(null);

        checkCameraPermissionAndStart();
    }


    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // 🔥 Use the new Reusable Analyzer!
        imageAnalysis.setAnalyzer(cameraExecutor, new ReusableBarcodeAnalyzer(this, isProcessing));

        cameraProvider.unbindAll();

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    // --- 3. BARCODE CALLBACK AND PROCESSING ---

    @Override
    public void onBarcodeScanned(String rawValue) {
        processScannedBarcode(rawValue);
    }

    private void processScannedBarcode(String rawValue) {
        requireActivity().runOnUiThread(() -> {
            if (rawValue != null && !rawValue.equals(lastScannedBarcode)) {

                lastScannedBarcode = rawValue;

                Product foundProduct = dbHelper.getProductByBarcode(rawValue);

                // 🔥 CRITICAL DEBUG LOG ADDED HERE 🔥
                if (foundProduct != null) {
                    Log.d(TAG, "Scanned Product: " + foundProduct.getName() +
                            " | Barcode: " + rawValue +
                            " | Stock Retrieved: " + foundProduct.getStockQuantity());
                } else {
                    Log.e(TAG, "Scanned Barcode: " + rawValue + " not found in DB.");
                }
                // 🔥 END DEBUG LOG 🔥


                if (foundProduct != null) {
                    updateProductUI(foundProduct);
                } else {
                    resetProductUI(rawValue);
                    Toast.makeText(getContext(), "Product not found in stock", Toast.LENGTH_SHORT).show();
                }

                scanCooldownHandler.postDelayed(() -> lastScannedBarcode = null, SCAN_COOLDOWN_MS);
            }
        });
    }

    // --- 4. UI UPDATE METHODS ---

    private void updateProductUI(Product product) {
        if (tvProductName != null) {
            tvProductName.setText(product.getName());
        }
        if (tvPrice != null) {
            tvPrice.setText(String.format("₱%.2f", product.getSellingPrice()));
        }

        String description = "Stock: " + product.getStockQuantity() + " units" +
                "\nCategory: " + product.getCategory() +
                "\nWeight: " + product.getWeight();

        if (tvDescription != null) {
            tvDescription.setText(description);
        }

        if(tvFooterMessage != null) {
            tvFooterMessage.setText("✅ Found: " + product.getName() + " | Stock: " + product.getStockQuantity());
        }
    }

    private void resetProductUI(@Nullable String barcode) {
        if (tvProductName != null) tvProductName.setText("_____________________________");
        if (tvPrice != null) tvPrice.setText("₱ __________");

        if (barcode != null) {
            if (tvDescription != null) tvDescription.setText("Product not found for barcode: " + barcode);
            if(tvFooterMessage != null) {
                tvFooterMessage.setText("❌ Error: Barcode " + barcode + " is not in stock.");
            }
        } else {
            if (tvDescription != null) tvDescription.setText("_____________________________");
            if(tvFooterMessage != null) {
                tvFooterMessage.setText("Scan a product to view its details");
            }
        }
    }


    // --- 5. CLEANUP ---

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // Assuming the ML Kit client is closed within the Analyzer's finalization or is stateless
        scanCooldownHandler.removeCallbacksAndMessages(null);
    }
}