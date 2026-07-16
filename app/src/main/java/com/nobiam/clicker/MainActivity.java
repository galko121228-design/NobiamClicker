package com.nobiam.clicker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private SeekBar seekbarCps;
    private TextView tvCpsValue, tvCurrentCps, tvClickerState, tvModeLabel;
    private ImageView ivStatusIndicator;
    private Switch switchMode;
    private MaterialButton btnGrantOverlay, btnGrantAccessibility, btnStartOverlay;
    private SharedPreferences prefs;
    private boolean isOverlayGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemBars();

        prefs = getSharedPreferences("NobiamPrefs", MODE_PRIVATE);

        seekbarCps = findViewById(R.id.seekbar_cps);
        tvCpsValue = findViewById(R.id.tv_cps_value);
        tvCurrentCps = findViewById(R.id.tv_current_cps);
        tvClickerState = findViewById(R.id.tv_clicker_state);
        tvModeLabel = findViewById(R.id.tv_mode_label);
        ivStatusIndicator = findViewById(R.id.iv_status_indicator);
        switchMode = findViewById(R.id.switch_mode);
        btnGrantOverlay = findViewById(R.id.btn_grant_overlay);
        btnGrantAccessibility = findViewById(R.id.btn_grant_accessibility);
        btnStartOverlay = findViewById(R.id.btn_start_overlay);

        checkPermissions();

        int savedCps = prefs.getInt("cps", 10);
        seekbarCps.setProgress(savedCps - 1);
        tvCpsValue.setText(String.valueOf(savedCps));
        tvCurrentCps.setText(String.valueOf(savedCps) + ".0");

        seekbarCps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int cps = progress + 1;
                tvCpsValue.setText(String.valueOf(cps));
                tvCurrentCps.setText(String.valueOf(cps) + ".0");
                prefs.edit().putInt("cps", cps).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvModeLabel.setText(isChecked ? "HOLD" : "TOGGLE");
            prefs.edit().putBoolean("hold_mode", isChecked).apply();
        });
        switchMode.setChecked(prefs.getBoolean("hold_mode", false));
        tvModeLabel.setText(switchMode.isChecked() ? "HOLD" : "TOGGLE");

        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            }
        });

        btnGrantAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnStartOverlay.setOnClickListener(v -> {
            if (!isOverlayGranted) {
                Toast.makeText(this, "Сначала дайте разрешение Overlay", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent serviceIntent = new Intent(this, FloatingButtonService.class);
            serviceIntent.putExtra("cps", seekbarCps.getProgress() + 1);
            startService(serviceIntent);
            btnStartOverlay.setText("OVERLAY ACTIVE");
            btnStartOverlay.setEnabled(false);
            Toast.makeText(this, "Оверлей запущен! Перетащите его пальцем", Toast.LENGTH_LONG).show();
        });
    }

    private void checkPermissions() {
        isOverlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(this);
        if (isOverlayGranted) {
            btnGrantOverlay.setText("✅ GRANTED");
            btnGrantOverlay.setEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            checkPermissions();
            if (isOverlayGranted) {
                btnGrantOverlay.setText("✅ GRANTED");
                btnGrantOverlay.setEnabled(false);
            }
        }
    }

    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), decorView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }
}
