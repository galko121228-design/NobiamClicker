package com.nobiam.clicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private Button btnGrantOverlay, btnStartMenu;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGrantOverlay = findViewById(R.id.btn_grant_overlay);
        btnStartMenu = findViewById(R.id.btn_start_overlay);
        tvStatus = findViewById(R.id.tv_status);

        checkPermissions();

        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            }
        });

        btnStartMenu.setOnClickListener(v -> {
            // ============================================================
            // ПРОВЕРКА через Settings.canDrawOverlays() — 100% рабочий способ
            // ============================================================
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "❌ Сначала дайте разрешение Overlay", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent serviceIntent = new Intent(this, FloatingMenuService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "✅ Оверлей запущен!", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPermissions() {
        boolean overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this);

        if (overlayGranted) {
            btnGrantOverlay.setText("✅ Overlay разрешено");
            btnGrantOverlay.setEnabled(false);
            tvStatus.setText("✅ Всё готово к работе");
            tvStatus.setTextColor(getColor(android.R.color.holo_green_light));
        } else {
            tvStatus.setText("❌ Дайте разрешение Overlay");
            tvStatus.setTextColor(getColor(android.R.color.holo_red_light));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            checkPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }
}
