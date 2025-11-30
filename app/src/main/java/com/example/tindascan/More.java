package com.example.tindascan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class More extends Fragment {

    private FirebaseSyncManager syncManager;

    public More() {
        super(R.layout.more);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set Action Bar Title
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("More");
        }

        // Initialize Firebase Manager
        syncManager = new FirebaseSyncManager(requireContext());

        // 1. Orders History
        View btnHistory = view.findViewById(R.id.card_orders_history);
        btnHistory.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_more_to_nav_history);
        });

        // 2. Settings (Placeholder)
        View btnSettings = view.findViewById(R.id.card_settings);
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // 3. Export Data (Upload to Firebase)
        View btnExport = view.findViewById(R.id.card_export);
        btnExport.setOnClickListener(v -> handleExport());

        // 4. Import Data (Download from Firebase)
        View btnImport = view.findViewById(R.id.card_import);
        btnImport.setOnClickListener(v -> handleImport());

        // 5. 🔥 NEW: Lock App Logic
        View btnLock = view.findViewById(R.id.card_lock_app);
        btnLock.setOnClickListener(v -> {
            // A. Set "is_app_locked" to TRUE in preferences
            SharedPreferences prefs = requireContext().getSharedPreferences("TindaScanSecurity", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_app_locked", true).apply();

            // B. Redirect to PinLoginActivity immediately
            Intent intent = new Intent(requireContext(), PinLoginActivity.class);
            // Clear the activity stack so pressing "Back" doesn't return to the app content
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            Toast.makeText(getContext(), "App Locked", Toast.LENGTH_SHORT).show();
        });

        // 6. Log Out
        View btnLogout = view.findViewById(R.id.card_logout);
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // 1. Sign out from Firebase
                        FirebaseAuth.getInstance().signOut();

                        // 2. Redirect to PinLoginActivity (which checks auth and will send to Register/Login)
                        Intent intent = new Intent(requireContext(), PinLoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // --- Helper Methods for Sync ---

    private void handleExport() {
        Toast.makeText(getContext(), "Backing up Products & Transactions...", Toast.LENGTH_SHORT).show();

        syncManager.exportAllData(new FirebaseSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleImport() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Restore Full Backup")
                .setMessage("This will download Products, Transaction History, and Sales details from the cloud.\n\nLocal data will be updated. Continue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Toast.makeText(getContext(), "Restoring data...", Toast.LENGTH_SHORT).show();

                    syncManager.importAllData(new FirebaseSyncManager.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}