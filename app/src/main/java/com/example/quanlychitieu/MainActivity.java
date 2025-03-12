package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quanlychitieu.auth.LoginActivity;
import com.example.quanlychitieu.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment not found");
            return;
        }

        navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d("NavController", "Navigating to: " + destination.getId());
        });

        // Define top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard,
                R.id.navigation_transactions,
                R.id.navigation_budget,
                R.id.navigation_statistics,
                R.id.navigation_reminders
        ).build();

        BottomNavigationView navView = binding.navView;

        // Setup bottom navigation handling
        setupBottomNavigation(navView);

        // Handle navigation destination changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            handleDestinationChange(destination);
        });

        // Setup FAB click listener
        binding.fabAddTransaction.setOnClickListener(v -> {
            // Navigate to add transaction screen
            if (navController.getCurrentDestination() != null) {
                if (navController.getCurrentDestination().getId() == R.id.navigation_dashboard) {
                    try {
                        navController.navigate(R.id.action_dashboard_to_add_transaction);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation failed: " + e.getMessage());
                    }
                } else if (navController.getCurrentDestination().getId() == R.id.navigation_transactions) {
                    try {
                        navController.navigate(R.id.action_transactions_to_add_transaction);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void handleDestinationChange(NavDestination destination) {
        Log.d(TAG, "Navigating to: " + destination.getId());
        int destinationId = destination.getId();

        // Only show FAB on dashboard and transactions screens
        binding.fabAddTransaction.setVisibility(destinationId == R.id.navigation_dashboard ||
                destinationId == R.id.navigation_transactions ? View.VISIBLE : View.GONE);
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigationView) {
        // Set up bottom navigation with NavController
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Handle reselection properly
        bottomNavigationView.setOnItemReselectedListener(item -> {
            int itemId = item.getItemId();
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == itemId) {
                // Pop back stack to the start destination of this tab
                navController.popBackStack(itemId, false);
            }
        });

        // Handle item selection with proper back stack handling
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (isTopLevelDestination(itemId)) {
                // Pop back stack to the start destination if needed
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != itemId) {
                    navController.popBackStack(itemId, false);
                }
                // Navigate to the selected destination
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
            return false;
        });
    }

    private boolean isTopLevelDestination(int itemId) {
        return itemId == R.id.navigation_dashboard ||
                itemId == R.id.navigation_transactions ||
                itemId == R.id.navigation_budget ||
                itemId == R.id.navigation_statistics ||
                itemId == R.id.navigation_reminders;
    }
}
