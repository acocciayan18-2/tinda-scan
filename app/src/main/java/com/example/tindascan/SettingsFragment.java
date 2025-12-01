package com.example.tindascan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "TindaScanSettings";
    private SharedPreferences prefs;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ImageButton btnBack = view.findViewById(R.id.btn_back_settings);
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        setupSwitch(view.findViewById(R.id.switch_inventory), "notify_inventory");
        setupSwitch(view.findViewById(R.id.switch_expiry), "notify_expiry");
        setupSwitch(view.findViewById(R.id.switch_reports), "notify_reports");
        setupSwitch(view.findViewById(R.id.switch_system), "notify_system");
    }

    private void setupSwitch(MaterialSwitch sw, String key) {
        // Set initial state (Default true)
        sw.setChecked(prefs.getBoolean(key, true));

        // Save on change
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).apply();
        });
    }
}