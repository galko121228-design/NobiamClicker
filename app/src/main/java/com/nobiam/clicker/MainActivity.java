package com.nobiam.clicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnGrantOverlay, btnGrantAccessibility, btnStartMenu;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGrantOverlay = findViewById(R.id.btn_grant_overlay);
        btnGrantAccessibility = findViewById(R.id.btn_grant_accessibility);
        btnStartMenu = findViewById(R.id.btn_start_overlay);
        tvStatus = findViewById(R.id.tv_status);

        // Проверяем разрешения при запуске
        checkPermissions();

        // Кнопка: разрешение Overlay
        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1001);
            }
        });

        // Кнопка: включить Accessibility
        btnGrantAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        // Кнопка: запустить оверлей-меню
        btnStartMenu.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "❌ Сначала дайте разрешение Overlay", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "❌ Сначала включите специальные возможности", Toast.LENGTH_LONG).show();
                return;
            }
            Intent serviceIntent = new Intent(this, FloatingMenuService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "✅ Оверлей-меню запущено", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPermissions() {
        boolean overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this);
        boolean accessibilityEnabled = isAccessibilityEnabled();

        if (overlayGranted) {
            btnGrantOverlay.setText("✅ Overlay разрешено");
            btnGrantOverlay.setEnabled(false);
        }

        if (accessibilityEnabled) {
            btnGrantAccessibility.setText("✅ Accessibility включено");
            btnGrantAccessibility.setEnabled(false);
        }

        tvStatus.setText(overlayGranted && accessibilityEnabled ? "✅ Всё готово к работе" : "❌ Дайте разрешения ниже");
    }

    private boolean isAccessibilityEnabled() {
        // Проверяем, включена ли специальная возможность для нашего приложения
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(getPackageName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            checkPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем статус при возвращении в приложение
        checkPermissions();
    }
}
