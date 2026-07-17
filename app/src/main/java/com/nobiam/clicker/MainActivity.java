package com.nobiam.clicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private SeekBar seekSpeed;
    private TextView txtSpeed;
    private Button btnOverlay, btnAccessibility, btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔥 УБИРАЕМ СИСТЕМНЫЕ БАРЫ
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        seekSpeed = findViewById(R.id.seekSpeed);
        txtSpeed = findViewById(R.id.txtSpeed);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnStart = findViewById(R.id.btnStart);

        // 🎯 Частота кликов
        seekSpeed.setMax(20);
        seekSpeed.setProgress(5);

        txtSpeed.setText("Скорость: 5 кликов/сек");

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1;
                txtSpeed.setText("Скорость: " + progress + " кликов/сек");

                getSharedPreferences("cfg", MODE_PRIVATE)
                        .edit()
                        .putInt("speed", progress)
                        .apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 🔐 Overlay разрешение
        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        // ⚡ Accessibility
        btnAccessibility.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        // 🚀 Запуск overlay сервиса
        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) return;

            Intent intent = new Intent(this, FloatingMenuService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        });
    }
}
