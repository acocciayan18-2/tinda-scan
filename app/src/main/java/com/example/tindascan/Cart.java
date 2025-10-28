package com.example.tindascan;



import android.Manifest;

import android.annotation.SuppressLint;

import android.content.pm.PackageManager;

import android.media.MediaPlayer;

import android.os.Bundle;

import android.os.Handler;

import android.os.Looper;

import android.util.Log;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

// import android.widget.Button; // Unused import

import android.widget.FrameLayout;
import android.widget.ImageButton;

import android.widget.LinearLayout;

import android.widget.TextView;

import android.widget.Toast;



import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.camera.core.CameraSelector;

import androidx.camera.core.ImageAnalysis;

import androidx.camera.core.Preview;

import androidx.camera.lifecycle.ProcessCameraProvider;

import androidx.camera.view.PreviewView;

import androidx.core.content.ContextCompat;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.button.MaterialButton;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.vision.barcode.BarcodeScanning;

import com.google.mlkit.vision.barcode.BarcodeScanner;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import com.google.mlkit.vision.barcode.common.Barcode;

import com.google.mlkit.vision.common.InputImage;



import java.util.ArrayList;

import java.util.HashSet;

import java.util.List;

import java.util.Set;

import java.util.concurrent.ExecutionException;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicBoolean;



public class Cart extends Fragment {



    private static final String TAG = "CartFragment";

    private PreviewView previewView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private ExecutorService cameraExecutor;

    private BarcodeScanner barcodeScanner;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private final Set<String> scannedBarcodes = new HashSet<>();

    private final Set<String> cartBarcodes = new HashSet<>();

    private DatabaseHelper dbHelper;



    private CartStorage cartStorage;

// ✅ CHANGED to List<CartItem>

    private List<CartItem> cartItems = new ArrayList<>();



    private MediaPlayer beepSound;

    private final Handler scanCooldownHandler = new Handler(Looper.getMainLooper());

    private static final long SCAN_COOLDOWN_MS = 2000;



    private MaterialButton btnClearCart;

    private LinearLayout cartItemsContainer;



    private final ActivityResultLauncher<String> requestPermissionLauncher =

            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {

                if (isGranted) {

                    startCamera();

                } else {

                    Toast.makeText(getContext(), "Camera permission is required to scan barcodes.", Toast.LENGTH_LONG).show();

                }

            });



    public Cart() {

        super(R.layout.cart);

    }



    @Override

    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();

        setupBarcodeScanner();

        dbHelper = new DatabaseHelper(requireContext());

    }



    @Nullable

    @Override

    public View onCreateView(@NonNull LayoutInflater inflater,

                             @Nullable ViewGroup container,

                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.cart, container, false);

    }



    @Override

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);



        cartItemsContainer = view.findViewById(R.id.cart_items_container);

        previewView = view.findViewById(R.id.camera_preview);

        beepSound = MediaPlayer.create(requireContext(), R.raw.beep);



        cartStorage = new CartStorage(requireContext());

        cartItems = cartStorage.loadCart(); // ✅ Loads List<CartItem>

        populateSavedCart();



        btnClearCart = view.findViewById(R.id.btn_clear_cart);

        updateClearButtonVisibility();



        btnClearCart.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Cart")
                    .setMessage("Are you sure you want to remove all items from the cart?")
                    .setIcon(R.drawable.ic_trash_stroke) // optional
                    .setPositiveButton("Yes", (dialog, which) -> {
                        clearCart(); // <-- your method to clear the cart
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });






        checkCameraPermissionAndStart();

        MaterialButton btnProceed = view.findViewById(R.id.btn_proceed);




        btnProceed.setOnClickListener(v -> {

            if (cartItems.isEmpty()) {

                return;
            }
            NavHostFragment.findNavController(this)
                    .navigate(R.id.nav_checkout);
        });




    }



// ... (checkCameraPermissionAndStart, startCamera, bindPreviewAndAnalysis, setupBarcodeScanner, BarcodeAnalyzer are all unchanged) ...

    private void checkCameraPermissionAndStart() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)

                == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            requestPermissionLauncher.launch(Manifest.permission.CAMERA);

        }

    }



    private void startCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                bindPreviewAndAnalysis(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {

                Log.e(TAG, "Error starting camera: ", e);

            }

        }, ContextCompat.getMainExecutor(requireContext()));

    }

    private void clearCart() {
        // 1. Clear the data list in memory
        cartItems.clear();

        // 2. Clear the persistent storage
        cartStorage.clearCart();

        // 3. Clear the duplicate-check set
        cartBarcodes.clear();

        // 4. This is the FIX: Remove all views from the LinearLayout
        cartItemsContainer.removeAllViews();

        // 5. Update the UI state
        updateClearButtonVisibility(); // Hides the "Clear Cart" button
        // updateTotalPrice(); // Add this back when you implement it

        // 6. Show confirmation
        Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show();
    }




    private void updateClearButtonVisibility() {

        if (btnClearCart != null) {

            if (cartItems.isEmpty()) {

                btnClearCart.setVisibility(View.GONE);

            } else {

                btnClearCart.setVisibility(View.VISIBLE);

            }

        }

    }



// In your Cart.java file



    private void populateSavedCart() {

        cartItemsContainer.removeAllViews();

        cartBarcodes.clear();



// We'll collect any bad items and remove them after the loop

        List<CartItem> itemsToRemove = new ArrayList<>();



        for (CartItem cartItem : cartItems) {



// --- ✅ SAFETY CHECK ---

// Get the product *and check if it's null*

            Product product = cartItem.getProduct();

            if (product == null) {

                Log.e(TAG, "Found a CartItem with a null Product, scheduling for removal.");

                itemsToRemove.add(cartItem); // Add to a list to remove later

                continue; // Skip this item

            }

// --- END SAFETY CHECK ---



            buildCartItemView(cartItem); // Build the UI for each item



// This line (was line 168) is now safe because product is not null

            cartBarcodes.add(product.getBarcode());

        }



// Remove any bad items we found from the main list

        if (!itemsToRemove.isEmpty()) {

            cartItems.removeAll(itemsToRemove);

            cartStorage.saveCart(cartItems); // Save the cleaned-up list

        }

    }





    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()

                .requireLensFacing(CameraSelector.LENS_FACING_BACK)

                .build();



        preview.setSurfaceProvider(previewView.getSurfaceProvider());



        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()

                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                .build();



        imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());

        cameraProvider.unbindAll();



        try {

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {

            Log.e(TAG, "Use case binding failed", e);

        }

    }



    private void setupBarcodeScanner() {

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().build();

        barcodeScanner = BarcodeScanning.getClient(options);

    }



    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

        @Override

        @SuppressLint("UnsafeOptInUsageError")

        public void analyze(@NonNull androidx.camera.core.ImageProxy imageProxy) {

            if (!isProcessing.compareAndSet(false, true)) {

                imageProxy.close();

                return;

            }



            android.media.Image mediaImage = imageProxy.getImage();

            if (mediaImage != null) {

                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                barcodeScanner.process(image)

                        .addOnSuccessListener(barcodes -> {

                            processBarcodeResults(barcodes);

                            imageProxy.close();

                            isProcessing.set(false);

                        })

                        .addOnFailureListener(e -> {

                            Log.e(TAG, "Barcode scanning failed", e);

                            imageProxy.close();

                            isProcessing.set(false);

                        });

            } else {

                imageProxy.close();

                isProcessing.set(false);

            }

        }

    }

// ...



    private void processBarcodeResults(List<Barcode> barcodes) {

        if (!barcodes.isEmpty()) {

            Barcode barcode = barcodes.get(0);

            String rawValue = barcode.getRawValue();



            if (rawValue != null && !scannedBarcodes.contains(rawValue)) {

                scannedBarcodes.add(rawValue);



                requireActivity().runOnUiThread(() -> {

                    Product foundProduct = dbHelper.getProductByBarcode(rawValue);



                    if (foundProduct != null) {

                        if (cartBarcodes.contains(rawValue)) {

                            Toast.makeText(getContext(), "This product is already in the cart.", Toast.LENGTH_SHORT).show();

                        } else {

                            if (beepSound != null) beepSound.start();



// ✅ CHANGED to create and add a CartItem

                            CartItem newCartItem = new CartItem(foundProduct, 1);

                            cartItems.add(newCartItem);

                            cartStorage.saveCart(cartItems); // Save the list

                            cartBarcodes.add(rawValue);

                            updateClearButtonVisibility();

                            buildCartItemView(newCartItem); // Build the UI



                            Toast.makeText(getContext(),

                                    "Added: " + foundProduct.getName(),

                                    Toast.LENGTH_SHORT).show();

                        }

                    } else {

                        Toast.makeText(getContext(), "Product not found in stock", Toast.LENGTH_SHORT).show();

                    }



                    scanCooldownHandler.postDelayed(() -> scannedBarcodes.remove(rawValue), SCAN_COOLDOWN_MS);

                });

            }

        }

    }



    /**

     * ✅ CHANGED to accept CartItem

     */

    private void removeItemFromCart(CartItem cartItem, View itemView) {

        cartItems.remove(cartItem);

        cartBarcodes.remove(cartItem.getProduct().getBarcode());

        cartStorage.saveCart(cartItems); // Save changes



        cartItemsContainer.removeView(itemView);

        updateClearButtonVisibility();



        Toast.makeText(getContext(), "Removed: " + cartItem.getProduct().getName(), Toast.LENGTH_SHORT).show();

    }



    /**

     * ✅ CHANGED to accept CartItem and handle saving quantity

     */

    private void buildCartItemView(CartItem cartItem) {

        Product product = cartItem.getProduct(); // Get the product from the item



        if (product == null) {

            Log.e(TAG, "A CartItem had a null Product. Removing it.");

// We can't show a view, so just remove the bad item

            cartItems.remove(cartItem);

            cartStorage.saveCart(cartItems);

            return;

        }



        LayoutInflater inflater = LayoutInflater.from(getContext());

        View itemView = inflater.inflate(R.layout.item_cart_product, cartItemsContainer, false);



        TextView tvName = itemView.findViewById(R.id.tv_item_name);

        TextView tvPrice = itemView.findViewById(R.id.tv_item_price);

        TextView tvQuantity = itemView.findViewById(R.id.tv_quantity);

        ImageButton btnMinus = itemView.findViewById(R.id.btn_quantity_minus);

        ImageButton btnPlus = itemView.findViewById(R.id.btn_quantity_plus);



        tvName.setText(product.getName());

        tvPrice.setText("₱" + String.format("%.2f", product.getPrice()));



// ✅ KEY FIX: Set quantity from the saved CartItem

        tvQuantity.setText(String.valueOf(cartItem.getQuantity()));



        btnPlus.setOnClickListener(v -> {

            int qty = Integer.parseInt(tvQuantity.getText().toString());

            if (qty + 1 > product.getStockQuantity()) {

                Toast.makeText(getContext(), "Stock limit reached for " + product.getName(), Toast.LENGTH_SHORT).show();

            } else {

                qty++;

                tvQuantity.setText(String.valueOf(qty));



// ✅ KEY FIX: Save the new quantity

                cartItem.setQuantity(qty);

                cartStorage.saveCart(cartItems);

            }

        });



        btnMinus.setOnClickListener(v -> {

            int qty = Integer.parseInt(tvQuantity.getText().toString());

            if (qty > 1) {

                qty--;

                tvQuantity.setText(String.valueOf(qty));



// ✅ KEY FIX: Save the new quantity

                cartItem.setQuantity(qty);

                cartStorage.saveCart(cartItems);

            } else {

// Quantity is 1, so remove the item

                removeItemFromCart(cartItem, itemView);

            }

        });



        cartItemsContainer.addView(itemView);

    }





    @Override

    public void onDestroyView() {

        super.onDestroyView();

        if (beepSound != null) {

            beepSound.release();

            beepSound = null;

        }

    }



    @Override

    public void onDestroy() {

        super.onDestroy();

        if (cameraExecutor != null) {

            cameraExecutor.shutdown();

        }

        if (barcodeScanner != null) {

            barcodeScanner.close();

        }

    }

}



