package com.example.tindascan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

public class More extends Fragment {

    private FirebaseSyncManager syncManager;

    // UI components for the Progress Dialog
    private AlertDialog progressDialog;
    private TextView tvProgressTitle, tvProgressMessage;
    private ProgressBar progressBar;
    private Button btnCancelSync;

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
            // Ensure 'nav_history' exists in your nav_graph.xml
            try {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_more_to_nav_history);
            } catch (Exception e) {
                // Fallback if action is missing, try direct navigation
                NavHostFragment.findNavController(this).navigate(R.id.nav_history);
            }
        });

        // 2. Settings
        View btnSettings = view.findViewById(R.id.card_settings);
        btnSettings.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.nav_settings);
        });

        // 3. Export Data
        View btnExport = view.findViewById(R.id.card_export);
        btnExport.setOnClickListener(v -> handleExport());

        // 4. Import Data
        View btnImport = view.findViewById(R.id.card_import);
        btnImport.setOnClickListener(v -> handleImport());

        // 5. Lock App Logic
        View btnLock = view.findViewById(R.id.card_lock_app);
        btnLock.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences("TindaScanSecurity", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_app_locked", true).apply();

            Intent intent = new Intent(requireContext(), PinLoginActivity.class);
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
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(requireContext(), PinLoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }


    private void showProgressDialog(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sync_progress, null);

        tvProgressTitle = view.findViewById(R.id.tv_progress_title);
        tvProgressMessage = view.findViewById(R.id.tv_progress_message);
        progressBar = view.findViewById(R.id.progressBar);
        btnCancelSync = view.findViewById(R.id.btn_cancel_sync);

        tvProgressTitle.setText(title);
        if (progressBar != null) progressBar.setProgress(0);

        btnCancelSync.setOnClickListener(v -> {
            if (tvProgressMessage != null) tvProgressMessage.setText("Cancelling... Rolling back changes...");
            btnCancelSync.setEnabled(false);
            syncManager.cancelProcess();
        });

        builder.setView(view);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleExport() {
        showProgressDialog("Backing up Data");

        syncManager.exportAllData(new FirebaseSyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                dismissProgressDialog();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                dismissProgressDialog();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onProgress(String status, int percent) {
                if (tvProgressMessage != null) tvProgressMessage.setText(status);
                if (progressBar != null) progressBar.setProgress(percent);
            }
        });
    }

    private void handleImport() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Restore Full Backup")
                .setMessage("This will download Products, Transaction History, and Sales details from the cloud.\n\nLocal data will be updated. Continue?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    showProgressDialog("Restoring Data");

                    syncManager.importAllData(new FirebaseSyncManager.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            dismissProgressDialog();
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "✅ " + message, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            dismissProgressDialog();
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "❌ " + error, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onProgress(String status, int percent) {
                            if (tvProgressMessage != null) tvProgressMessage.setText(status);
                            if (progressBar != null) progressBar.setProgress(percent);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}