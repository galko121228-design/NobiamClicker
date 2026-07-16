package com.nobiam.clicker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
    private TextView cpsText, statusText;
    private Button btnStartService, btnSetCoords;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Убираем системные бары
        hideSystemBars();

        prefs = getSharedPreferences("NobiamPrefs", MODE_PRIVATE);

        cpsSeekBar = findViewById(R.id.cpsSeekBar);
        cpsText = findViewById(R.id.cpsText);
        statusText = findViewById(R.id.statusText);
        btnStartService = findViewById(R.id.btnStartService);
        btnSetCoords = findViewById(R.id.btnSetCoords);

        // Загружаем сохранённые координаты
        int savedX = prefs.getInt("click_x", -1);
        int savedY = prefs.getInt("click_y", -1);
        if (savedX != -1 && savedY != -1) {
            statusText.setText("Координаты: " + savedX + "x" + savedY);
        } else {
            statusText.setText("Нажмите 'Запомнить координаты'");
        }

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

        btnSetCoords.setOnClickListener(v -> {
            Toast.makeText(this, "Откройте Minecraft и нажмите по кнопке атаки", Toast.LENGTH_LONG).show();
            // Здесь будет логика запоминания координат
            // Пока просто заглушка для демонстрации
            prefs.edit().putInt("click_x", 800).putInt("click_y", 800).apply();
            statusText.setText("Координаты сохранены: 800x800");
        });
    }

    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), decorView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
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
            hideSystemBars();
        }
    }
}
