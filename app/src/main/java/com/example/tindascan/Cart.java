package com.example.tindascan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import androidx.appcompat.widget.SearchView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cart extends Fragment implements ReusableBarcodeAnalyzer.OnBarcodeScannedListener { // <-- IMPLEMENTS REUSABLE ANALYZER

    private static final String TAG = "CartFragment";
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false); // Used by Analyzer
    private final Set<String> scannedBarcodes = new HashSet<>();
    private final Set<String> cartBarcodes = new HashSet<>();
    private DatabaseHelper dbHelper;
    private CartStorage cartStorage;
    private List<CartItem> cartItems = new ArrayList<>();
    private MediaPlayer beepSound;
    private final Handler scanCooldownHandler = new Handler(Looper.getMainLooper());
    private static final long SCAN_COOLDOWN_MS = 2000;
    private MaterialButton btnClearCart;
    private LinearLayout cartItemsContainer;
    private SearchView manualSearchView;

    private TextView tvEmptyCart;
    private TextView tvTotalPrice;

    // --- VARIABLES FOR IN-LINE SEARCH ---
    private ListView searchResultsList;
    private ArrayAdapter<String> searchAdapter;
    private List<Product> currentSearchResults = new ArrayList<>();

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
        // ... (Binding views and setup remains the same) ...
        cartItemsContainer = view.findViewById(R.id.cart_items_container);
        previewView = view.findViewById(R.id.camera_preview);

        tvEmptyCart = view.findViewById(R.id.tv_empty_cart);
        tvTotalPrice = view.findViewById(R.id.tv_total_price);

        beepSound = MediaPlayer.create(requireContext(), R.raw.beep);

        searchResultsList = view.findViewById(R.id.search_results_list);
        searchAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        searchResultsList.setAdapter(searchAdapter);
        searchResultsList.setVisibility(View.GONE);

        // ... (Item Click Listeners) ...

        searchResultsList.setOnItemClickListener((parent, view1, position, id) -> {
            if (currentSearchResults.isEmpty() || position >= currentSearchResults.size()) {
                manualSearchView.setQuery("", false);
                manualSearchView.clearFocus();
                searchResultsList.setVisibility(View.GONE);
                return;
            }

            Product selectedProduct = currentSearchResults.get(position);
            manualAddToCart(selectedProduct);

            manualSearchView.setQuery("", false);
            manualSearchView.clearFocus();
            searchResultsList.setVisibility(View.GONE);
        });

        cartStorage = new CartStorage(requireContext());
        cartItems = cartStorage.loadCart();

        btnClearCart = view.findViewById(R.id.btn_clear_cart);

        populateSavedCart();
        updateCartState();
        calculateAndUpdateTotal();

        btnClearCart.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Cart")
                    .setMessage("Are you sure you want to remove all items from the cart?")
                    .setIcon(R.drawable.ic_trash_stroke)
                    .setPositiveButton("Yes", (dialog, which) -> clearCart())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        manualSearchView = view.findViewById(R.id.manual_search_view);
        manualSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { displaySearchResults(query); manualSearchView.clearFocus(); return true; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    displaySearchResults(newText);
                } else {
                    searchResultsList.setVisibility(View.GONE);
                    currentSearchResults.clear();
                    searchAdapter.clear();
                }
                return true;
            }
        });

        manualSearchView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && manualSearchView.getQuery().length() == 0) {
                searchResultsList.setVisibility(View.GONE);
            }
        });

        checkCameraPermissionAndStart();

        MaterialButton btnProceed = view.findViewById(R.id.btn_proceed);
        btnProceed.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            NavHostFragment.findNavController(this).navigate(R.id.nav_checkout);
        });
    }

    private void updateCartState() {
    }

    // ... (calculateAndUpdateTotal, updateCartState, displaySearchResults, manualAddToCart, findItemView remain the same) ...

    private void calculateAndUpdateTotal() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            // Note: This relies on CartItem having a correctly populated Product object
            if (item.getProduct() != null) {
                total += item.getProduct().getSellingPrice() * item.getQuantity();
            }
        }
        if (tvTotalPrice != null) {
            tvTotalPrice.setText(String.format("₱%.2f", total));
        }
    }

    // ... (updateCartState remains the same) ...

    private void displaySearchResults(String query) {
        String trimmedQuery = query.trim();

        if (trimmedQuery.isEmpty()) {
            searchAdapter.clear();
            searchResultsList.setVisibility(View.GONE);
            currentSearchResults.clear();
            return;
        }

        currentSearchResults = dbHelper.getProductsByNameOrBarcode(trimmedQuery);
        searchAdapter.clear();

        if (currentSearchResults.isEmpty()) {
            searchAdapter.add("No products found matching \"" + trimmedQuery + "\"");
            searchResultsList.setVisibility(View.VISIBLE);
        } else {
            for (Product product : currentSearchResults) {
                String price = String.format("₱%.2f", product.getPrice());
                String stock = String.valueOf(product.getStockQuantity());
                searchAdapter.add(product.getName() + " (" + price + ") - Stock: " + stock);
            }
            searchResultsList.setVisibility(View.VISIBLE);
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void manualAddToCart(Product foundProduct) {
        String rawValue = foundProduct.getBarcode();

        if (foundProduct.getStockQuantity() <= 0) {
            Toast.makeText(getContext(), "Product Out of Order (Zero Stock): " + foundProduct.getName(), Toast.LENGTH_LONG).show();
            return;
        }

        if (cartBarcodes.contains(rawValue)) {
            CartItem existingItem = null;
            for (CartItem item : cartItems) {
                if (item.getProduct().getBarcode().equals(rawValue)) {
                    existingItem = item;
                    break;
                }
            }

            if (existingItem != null && existingItem.getQuantity() >= foundProduct.getStockQuantity()) {
                Toast.makeText(getContext(), "Stock limit reached for " + foundProduct.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (existingItem != null) {
                if (beepSound != null) beepSound.start();

                existingItem.setQuantity(existingItem.getQuantity() + 1);
                cartStorage.saveCart(cartItems);

                View itemView = findItemView(existingItem);
                if (itemView != null) {
                    TextView tvQuantity = itemView.findViewById(R.id.tv_quantity);
                    tvQuantity.setText(String.valueOf(existingItem.getQuantity()));
                }

                calculateAndUpdateTotal();
                Toast.makeText(getContext(), "Quantity +1 for: " + foundProduct.getName(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (beepSound != null) beepSound.start();

        CartItem newCartItem = new CartItem(foundProduct, 1);
        cartItems.add(newCartItem);
        cartStorage.saveCart(cartItems);
        cartBarcodes.add(rawValue);

        updateCartState();
        buildCartItemView(newCartItem);
        calculateAndUpdateTotal();

        Toast.makeText(getContext(), "Added: " + foundProduct.getName() + " manually.", Toast.LENGTH_SHORT).show();
    }

    private View findItemView(CartItem item) {
        for (int i = 0; i < cartItemsContainer.getChildCount(); i++) {
            View itemView = cartItemsContainer.getChildAt(i);
            Object tag = itemView.getTag();
            if (tag != null && tag instanceof String && tag.equals(item.getProduct().getBarcode())) {
                return itemView;
            }
        }
        return null;
    }

    // ... (checkCameraPermissionAndStart, startCamera, clearCart, populateSavedCart remain the same) ...

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
        cartItems.clear();
        cartStorage.clearCart();
        cartBarcodes.clear();
        cartItemsContainer.removeAllViews();

        updateCartState();
        calculateAndUpdateTotal();
        Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show();
    }

    private void populateSavedCart() {
        cartItemsContainer.removeAllViews();
        cartBarcodes.clear();

        List<CartItem> itemsToRemove = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null) {
                itemsToRemove.add(cartItem);
                continue;
            }
            buildCartItemView(cartItem);
            cartBarcodes.add(product.getBarcode());
        }

        if (!itemsToRemove.isEmpty()) {
            cartItems.removeAll(itemsToRemove);
            cartStorage.saveCart(cartItems);
        }
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        // 🔥 Use the new Reusable Analyzer!
        imageAnalysis.setAnalyzer(cameraExecutor, new ReusableBarcodeAnalyzer(this, isProcessing));

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

    // --- BARCODE ANALYZER CALLBACK ---
    @Override
    public void onBarcodeScanned(String rawValue) {
        processScannedBarcode(rawValue);
    }

    private void processScannedBarcode(String rawValue) {
        if (rawValue != null && !scannedBarcodes.contains(rawValue)) {
            scannedBarcodes.add(rawValue);

            requireActivity().runOnUiThread(() -> {
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
                    if (foundProduct.getStockQuantity() <= 0) {
                        Toast.makeText(getContext(), "Product Out of Order (Zero Stock): " + foundProduct.getName(), Toast.LENGTH_LONG).show();
                    } else if (cartBarcodes.contains(rawValue)) {
                        CartItem existingItem = null;
                        for (CartItem item : cartItems) {
                            if (item.getProduct().getBarcode().equals(rawValue)) {
                                existingItem = item;
                                break;
                            }
                        }
                        if (existingItem != null && existingItem.getQuantity() < foundProduct.getStockQuantity()) {
                            if (beepSound != null) beepSound.start();
                            existingItem.setQuantity(existingItem.getQuantity() + 1);
                            cartStorage.saveCart(cartItems);
                            View itemView = findItemView(existingItem);
                            if (itemView != null) {
                                TextView tvQuantity = itemView.findViewById(R.id.tv_quantity);
                                tvQuantity.setText(String.valueOf(existingItem.getQuantity()));
                            }
                            calculateAndUpdateTotal();
                            Toast.makeText(getContext(), "Quantity +1 for: " + foundProduct.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Stock limit reached or product not found in cart list.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (beepSound != null) beepSound.start();
                        CartItem newCartItem = new CartItem(foundProduct, 1);
                        cartItems.add(newCartItem);
                        cartStorage.saveCart(cartItems);
                        cartBarcodes.add(rawValue);

                        updateCartState();
                        buildCartItemView(newCartItem);
                        calculateAndUpdateTotal();
                    }
                } else {
                    Toast.makeText(getContext(), "Product not found in stock", Toast.LENGTH_SHORT).show();
                }
                scanCooldownHandler.postDelayed(() -> scannedBarcodes.remove(rawValue), SCAN_COOLDOWN_MS);
            });
        }
    }

    private void removeItemFromCart(CartItem cartItem, View itemView) {
        cartItems.remove(cartItem);
        cartBarcodes.remove(cartItem.getProduct().getBarcode());
        cartStorage.saveCart(cartItems);
        cartItemsContainer.removeView(itemView);

        updateCartState();
        calculateAndUpdateTotal();

        Toast.makeText(getContext(), "Removed: " + cartItem.getProduct().getName(), Toast.LENGTH_SHORT).show();
    }

    private void buildCartItemView(CartItem cartItem) {
        Product product = cartItem.getProduct();

        if (product == null) {
            cartItems.remove(cartItem);
            cartStorage.saveCart(cartItems);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View itemView = inflater.inflate(R.layout.item_cart_product, cartItemsContainer, false);

        itemView.setTag(product.getBarcode());

        TextView tvName = itemView.findViewById(R.id.tv_item_name);
        TextView tvPrice = itemView.findViewById(R.id.tv_item_price);
        TextView tvQuantity = itemView.findViewById(R.id.tv_quantity);
        ImageButton btnMinus = itemView.findViewById(R.id.btn_quantity_minus);
        ImageButton btnPlus = itemView.findViewById(R.id.btn_quantity_plus);

        tvName.setText(product.getName());
        tvPrice.setText("₱" + String.format("%.2f", product.getPrice()));
        tvQuantity.setText(String.valueOf(cartItem.getQuantity()));

        btnPlus.setOnClickListener(v -> {
            int qty = Integer.parseInt(tvQuantity.getText().toString());
            if (qty + 1 > product.getStockQuantity()) {
                Toast.makeText(getContext(), "Stock limit reached for " + product.getName(), Toast.LENGTH_SHORT).show();
            } else {
                qty++;
                tvQuantity.setText(String.valueOf(qty));
                cartItem.setQuantity(qty);
                cartStorage.saveCart(cartItems);
                calculateAndUpdateTotal();
            }
        });

        btnMinus.setOnClickListener(v -> {
            int qty = Integer.parseInt(tvQuantity.getText().toString());
            if (qty > 1) {
                qty--;
                tvQuantity.setText(String.valueOf(qty));
                cartItem.setQuantity(qty);
                cartStorage.saveCart(cartItems);
                calculateAndUpdateTotal();
            } else {
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