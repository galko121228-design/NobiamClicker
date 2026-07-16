package com.nobiam.clicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private SeekBar cpsSeekBar;
    private TextView cpsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== УБИРАЕМ СИСТЕМНЫЕ БАРЫ (НОВЫЙ СПОСОБ) =====
        hideSystemBars();

        cpsSeekBar = findViewById(R.id.cpsSeekBar);
        cpsText = findViewById(R.id.cpsText);
        Button btnStartService = findViewById(R.id.btnStartService);

        cpsSeekBar.setProgress(10);
        cpsText.setText("CPS: 10");

        cpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int cps = progress + 1;
                cpsText.setText("CPS: " + cps);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnStartService.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            } else {
                startFloatingService();
            }
        });
    }

    // ===== НОВЫЙ МЕТОД УБИРАНИЯ СИСТЕМНЫХ БАРОВ =====
    private void hideSystemBars() {
        // 1. Включаем режим edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Делаем бары прозрачными
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // 3. Прячем бары через WindowInsetsControllerCompat (новый API)
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decorView);

        // Скрываем системные бары
        controller.hide(WindowInsetsCompat.Type.systemBars());

        // Настройка поведения: появляются при свайпе, затем снова скрываются
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        serviceIntent.putExtra("cps", cpsSeekBar.getProgress() + 1);
        startService(serviceIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "Разрешение поверх экрана нужно для работы", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Если окно снова в фокусе — скрываем бары
            hideSystemBars();
        }
    }
}
