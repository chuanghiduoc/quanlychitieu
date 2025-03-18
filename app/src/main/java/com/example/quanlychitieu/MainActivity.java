package com.example.quanlychitieu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quanlychitieu.auth.LoginActivity;
import com.example.quanlychitieu.data.repository.TransactionRepository;
import com.example.quanlychitieu.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";
    private FirebaseAuth auth;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;

    // Launcher để yêu cầu quyền thông báo
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    showNotificationPermissionRationale();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        checkStoragePermission();
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
                    navController.navigate(R.id.action_dashboard_to_add_transaction);
                } else if (navController.getCurrentDestination().getId() == R.id.navigation_transactions) {
                    navController.navigate(R.id.action_transactions_to_add_transaction);
                } else if (navController.getCurrentDestination().getId() == R.id.navigation_reminders) {
                    navController.navigate(R.id.action_reminders_to_add_reminder);
                }
            }
        });

        // Xử lý intent từ notification
        handleNotificationIntent(getIntent());

        // Đặt context cho TransactionRepository
        TransactionRepository.getInstance().setContext(getApplicationContext());

        // Kiểm tra quyền thông báo
        checkNotificationPermission();
    }

    /**
     * Kiểm tra và yêu cầu quyền thông báo nếu cần
     */
    private void checkNotificationPermission() {
        // Quyền POST_NOTIFICATIONS chỉ cần thiết từ Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Yêu cầu quyền trực tiếp
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version");
        }
    }

    /**
     * Hiển thị giải thích về lý do cần quyền thông báo
     */
    private void showNotificationPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Quyền thông báo")
                .setMessage("Ứng dụng cần quyền thông báo để cảnh báo khi bạn chi tiêu vượt quá ngân sách. " +
                        "Vui lòng cấp quyền trong cài đặt ứng dụng.")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    // Mở cài đặt ứng dụng
                    openAppSettings();
                })
                .setNegativeButton("Để sau", (dialog, which) -> {
                    dialog.dismiss();
                    // Hiển thị Snackbar để nhắc nhở người dùng
                    Toast.makeText(this, "Bạn sẽ không nhận được thông báo khi vượt ngân sách",
                            Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Mở cài đặt ứng dụng
     */
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("REMINDER_ID")) {
            long reminderId = intent.getLongExtra("REMINDER_ID", -1);
            String documentId = intent.getStringExtra("REMINDER_DOCUMENT_ID");

            if (reminderId != -1) {
                Log.d(TAG, "Handling notification for reminder: " + reminderId);

                // Đảm bảo chuyển đến màn hình nhắc nhở trước
                navController.navigate(R.id.navigation_reminders);

                // Sau đó mở chi tiết nhắc nhở với ID
                Bundle args = new Bundle();
                args.putLong("reminderId", reminderId);
                args.putString("documentId", documentId);
                navController.navigate(R.id.action_reminders_to_add_reminder, args);
            }
        }
    }

    private void handleDestinationChange(NavDestination destination) {
        int destinationId = destination.getId();

        // Hiển thị FAB ở màn hình dashboard, transactions và reminders
        boolean shouldShowFab = (destinationId == R.id.navigation_dashboard ||
                destinationId == R.id.navigation_transactions ||
                destinationId == R.id.navigation_reminders);

        binding.fabAddTransaction.setVisibility(shouldShowFab ? View.VISIBLE : View.GONE);

        // Thay đổi icon của FAB tùy theo màn hình
        if (destinationId == R.id.navigation_reminders) {
            binding.fabAddTransaction.setImageResource(R.drawable.ic_notification);
        } else {
            binding.fabAddTransaction.setImageResource(R.drawable.ic_add);
        }
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
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Cần quyền lưu trữ để xuất báo cáo", Toast.LENGTH_LONG).show();
            }
        }
    }
    private boolean isTopLevelDestination(int itemId) {
        return itemId == R.id.navigation_dashboard ||
                itemId == R.id.navigation_transactions ||
                itemId == R.id.navigation_budget ||
                itemId == R.id.navigation_statistics ||
                itemId == R.id.navigation_reminders;
    }
}
