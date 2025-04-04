package com.example.quanlychitieu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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
    private ActivityResultLauncher<String[]> storagePermissionLauncher;
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

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        Log.d(TAG, "Storage permissions granted");
                        // Có thể thông báo cho các Fragment biết quyền đã được cấp
                    } else {
                        Log.d(TAG, "Storage permissions denied");
                        showStoragePermissionRationale();
                    }
                });

        // Kiểm tra quyền thông báo
        checkNotificationPermission();

        // Kiểm tra quyền lưu trữ
        checkStoragePermission();
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

    private boolean isTopLevelDestination(int itemId) {
        return itemId == R.id.navigation_dashboard ||
                itemId == R.id.navigation_transactions ||
                itemId == R.id.navigation_budget ||
                itemId == R.id.navigation_statistics ||
                itemId == R.id.navigation_reminders;
    }

    /**
     * Kiểm tra và yêu cầu quyền lưu trữ nếu cần
     */
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ sử dụng cơ chế lưu trữ phạm vi
            if (!Environment.isExternalStorageManager()) {
                // Hiển thị dialog giải thích trước khi yêu cầu quyền
                showStoragePermissionRationale();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 yêu cầu cấp quyền runtime
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Yêu cầu quyền trực tiếp
                storagePermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            } else {
                Log.d(TAG, "Storage permissions already granted");
            }
        } else {
            // Android 5.1 và thấp hơn không cần yêu cầu quyền runtime
            Log.d(TAG, "Storage permissions granted by default (old Android version)");
        }
    }

    /**
     * Hiển thị giải thích về lý do cần quyền lưu trữ
     */
    private void showStoragePermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Quyền lưu trữ")
                .setMessage("Ứng dụng cần quyền lưu trữ để xuất báo cáo tài chính. " +
                        "Vui lòng cấp quyền trong cài đặt ứng dụng.")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    // Mở cài đặt quyền dựa trên phiên bản Android
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            // Android 11+ yêu cầu cấp quyền quản lý tất cả tệp
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            // Fallback nếu không mở được intent cụ thể
                            openAppSettings();
                        }
                    } else {
                        openAppSettings();
                    }
                })
                .setNegativeButton("Để sau", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Bạn sẽ không thể xuất báo cáo tài chính",
                            Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    // Phương thức để kiểm tra nếu tất cả quyền lưu trữ đã được cấp
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Android 5.1 và thấp hơn không cần yêu cầu quyền runtime
        }
    }

    public void navigateToCategoryManagement() {
        if (navController != null) {
            try {
                navController.navigate(R.id.category_management_fragment);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to category management: " + e.getMessage());
                Toast.makeText(this, "Không thể mở màn hình quản lý danh mục", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
