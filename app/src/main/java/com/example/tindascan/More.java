package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;

public class More extends Fragment {
    public More() {
        super(R.layout.more);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Home");
        }

        // In MoreFragment.java, inside onViewCreated...
        // Change "MaterialButton" to "View" or "RelativeLayout"
        View btnHistory = view.findViewById(R.id.order_history_btn); // ✅ OK
        btnHistory.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_more_to_nav_history);
        });
    }
}
