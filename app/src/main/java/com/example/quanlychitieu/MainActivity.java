package com.example.quanlychitieu;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quanlychitieu.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        if (navHostFragment == null) {
            return;
        }

        navController = navHostFragment.getNavController();

        // Define top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard,
                R.id.navigation_transactions,
                R.id.navigation_budget,
                R.id.navigation_statistics,
                R.id.navigation_reminders,
                R.id.loginFragment, // Thêm màn hình đăng nhập vào top-level destinations
                R.id.registerFragment, // Thêm màn hình đăng ký
                R.id.profileFragment // Thêm màn hình profile
        ).build();

        BottomNavigationView navView = binding.navView;

        // Setup bottom navigation handling
        setupBottomNavigation(navView);

        // Thêm listener để không làm nổi bật BottomNavigationView khi điều hướng
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment ||
                    destination.getId() == R.id.registerFragment) {
                // Ẩn bottom navigation ở màn hình đăng nhập và đăng ký
                binding.navView.setVisibility(View.GONE);

                // Ẩn FAB (nếu cần)
                binding.fabAddTransaction.setVisibility(View.GONE);
            } else {
                // Hiển thị bottom navigation ở các màn hình khác
                binding.navView.setVisibility(View.VISIBLE);

                // Chỉ hiển thị FAB ở một số màn hình cụ thể (nếu cần)
                if (destination.getId() == R.id.navigation_dashboard ||
                        destination.getId() == R.id.navigation_transactions) {
                    binding.fabAddTransaction.setVisibility(View.VISIBLE);
                } else {
                    binding.fabAddTransaction.setVisibility(View.GONE);
                }
            }
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

    private void setupBottomNavigation(BottomNavigationView bottomNavigationView) {
        // Set up bottom navigation with NavController
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Handle reselection properly
        bottomNavigationView.setOnItemReselectedListener(item -> {
            int itemId = item.getItemId();

            // When re-selecting the current tab, pop the back stack to the start destination
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == itemId) {
                // Pop back stack to the start destination of this tab
                navController.popBackStack(itemId, false);
            }
        });

        // Handle item selection with proper back stack handling
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Handle navigation with proper back stack management
            int itemId = item.getItemId();

            // Check if we're navigating to a top-level destination
            if (itemId == R.id.navigation_dashboard ||
                    itemId == R.id.navigation_transactions ||
                    itemId == R.id.navigation_budget ||
                    itemId == R.id.navigation_statistics ||
                    itemId == R.id.navigation_reminders) {

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
}
