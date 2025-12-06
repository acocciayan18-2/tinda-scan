package com.example.tindascan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etStoreName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etStoreName = findViewById(R.id.et_store_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> handleRegister());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleRegister() {
        String storeName = etStoreName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        //  Validation
        if (TextUtils.isEmpty(storeName)) {
            etStoreName.setError("Store name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestore(user, storeName);
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String storeName) {
        if (user == null) return;

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUid());
        userMap.put("storeName", storeName);
        userMap.put("email", user.getEmail());
        userMap.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();


                    getSharedPreferences("TindaScanSecurity", MODE_PRIVATE).edit().clear().apply();

                    Intent intent = new Intent(RegisterActivity.this, PinLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Failed to save store details.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Creating Account..." : "Create Account & Set PIN");
    }
}