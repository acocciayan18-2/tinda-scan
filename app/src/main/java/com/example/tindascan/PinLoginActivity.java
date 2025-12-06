package com.example.tindascan;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PinLoginActivity extends AppCompatActivity {

    private StringBuilder currentPin = new StringBuilder();
    private View[] dots;
    private TextView tvTitle, tvMessage, tvForgotPin;

    private static final String PREF_NAME = "TindaScanSecurity";
    private static final String KEY_PIN = "user_pin";
    private static final String KEY_IS_LOCKED = "is_app_locked";

    private boolean isCreatingNewPin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_login);

        tvTitle = findViewById(R.id.tv_pin_title);
        tvMessage = findViewById(R.id.tv_pin_message);
        tvForgotPin = findViewById(R.id.tv_forgot_pin);

        // Initialize Dots
        dots = new View[]{
                findViewById(R.id.dot_1),
                findViewById(R.id.dot_2),
                findViewById(R.id.dot_3),
                findViewById(R.id.dot_4)
        };

        tvForgotPin.setOnClickListener(v -> handleForgotPin());

        checkState();
    }

    private void handleForgotPin() {
        new AlertDialog.Builder(this)
                .setTitle("Forgot PIN?")
                .setMessage("To reset your PIN, you need to verify your identity by logging in with your Email and Password again.\n\nContinue?")
                .setPositiveButton("Yes, Reset", (dialog, which) -> {
                    // 1. Clear the stored PIN and Lock state locally
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    // 2. Sign out of Firebase (Force Re-authentication)
                    FirebaseAuth.getInstance().signOut();

                    // 3. Go to Login Screen
                    Intent intent = new Intent(PinLoginActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    Toast.makeText(this, "Please log in to set a new PIN", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkState() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, null);
        boolean isLocked = prefs.getBoolean(KEY_IS_LOCKED, false);

        if (savedPin == null) {
            isCreatingNewPin = true;
            tvTitle.setText("Create PIN");
            tvMessage.setText("Set a 4-digit PIN for your store");
            tvForgotPin.setVisibility(View.INVISIBLE);
        } else if (isLocked) {
            isCreatingNewPin = false;
            tvTitle.setText("Welcome Back");
            tvMessage.setText("Enter PIN to access store");
            tvForgotPin.setVisibility(View.VISIBLE);
        } else {
            if (getIntent().getBooleanExtra("reset_mode", false)) {
                isCreatingNewPin = true;
                tvTitle.setText("Reset PIN");
                tvMessage.setText("Enter your new 4-digit PIN");
                tvForgotPin.setVisibility(View.INVISIBLE);
            } else {
                // Not locked, just go home
                goToHome();
            }
        }
    }

    // Called by XML onClick
    public void onKeyClick(View view) {
        if (currentPin.length() < 4) {
            Button b = (Button) view;
            currentPin.append(b.getText().toString());
            updateDots();

            if (currentPin.length() == 4) {
                verifyPin();
            }
        }
    }

    // Called by XML onClick
    public void onBackspaceClick(View view) {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            if (i < currentPin.length()) {
                dots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                dots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }
    }

    private void verifyPin() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (isCreatingNewPin) {
            // Save the new PIN
            prefs.edit().putString(KEY_PIN, currentPin.toString()).apply();
            prefs.edit().putBoolean(KEY_IS_LOCKED, false).apply(); // Unlock immediately
            Toast.makeText(this, "PIN Created Successfully!", Toast.LENGTH_SHORT).show();
            goToHome();
        } else {
            String savedPin = prefs.getString(KEY_PIN, "");
            if (currentPin.toString().equals(savedPin)) {
                prefs.edit().putBoolean(KEY_IS_LOCKED, false).apply(); // Set as unlocked
                goToHome();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                currentPin.setLength(0); // Clear input
                updateDots();
            }
        }
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}