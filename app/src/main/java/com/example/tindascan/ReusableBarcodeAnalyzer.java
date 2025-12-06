package com.example.tindascan;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.common.Barcode;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReusableBarcodeAnalyzer implements ImageAnalysis.Analyzer {

    private final OnBarcodeScannedListener listener;
    private final BarcodeScanner scanner;
    private final AtomicBoolean isProcessing; // Controls external processing state

    public interface OnBarcodeScannedListener {
        void onBarcodeScanned(String barcode);
    }

    public ReusableBarcodeAnalyzer(OnBarcodeScannedListener listener, AtomicBoolean isProcessingState) {
        this.listener = listener;
        this.isProcessing = isProcessingState;
        this.scanner = BarcodeScanning.getClient();
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String rawValue = barcodes.get(0).getRawValue();
                            listener.onBarcodeScanned(rawValue);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Analyzer", "Barcode scanning failed", e);
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                        isProcessing.set(false);
                    });
        } else {
            imageProxy.close();
            isProcessing.set(false);
        }
    }
}