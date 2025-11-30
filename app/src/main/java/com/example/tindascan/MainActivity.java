package com.example.tindascan;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;

// 🔥 IMPORT FIREBASE
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.SHOW_SPLASH) {
            SplashScreen.installSplashScreen(this);
        }
        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);

        // 🔥 INITIALIZE FIREBASE MANUALLY HERE
        // This prevents the "Default FirebaseApp is not initialized" crash
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            NavigationUI.setupWithNavController(navView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();

                // Hide bottom navigation on specific screens
                if (id == R.id.nav_add_product ||
                        id == R.id.nav_checkout ||
                        id == R.id.nav_details ||
                        id == R.id.nav_history ||
                        id == R.id.nav_edit_product) {

                    navView.setVisibility(View.GONE);
                } else {
                    navView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}