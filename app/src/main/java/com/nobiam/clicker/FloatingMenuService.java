package com.nobiam.clicker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class FloatingMenuService extends Service {

    private static final String CHANNEL_ID = "nobiam_overlay";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private SharedPreferences prefs;
    private View logoView;
    private View menuView;
    private View circleView;
    private Button macroButton;
    private WindowManager.LayoutParams logoParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams circleParams;
    private boolean isMenuOpen = false;
    private boolean isSettingTarget = false;
    private int targetX = 500, targetY = 800;
    private float density;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("NobiamPrefs", MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        density = getResources().getDisplayMetrics().density;

        targetX = prefs.getInt("target_x", 500);
        targetY = prefs.getInt("target_y", 800);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "❌ Нет разрешения Overlay", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        createLogo();
        createMenu();
        hideMenu();

        if (prefs.getBoolean("has_macro", false)) {
            createMacroButton();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nobiam Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Оверлей для автоматизации");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⚔️ Nobiam Clicker")
                .setContentText("Нажмите на иконку ⚔️ для меню")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private int dp(int px) {
        return (int) (px * density);
    }

    private void createLogo() {
        LinearLayout logoLayout = new LinearLayout(this);
        logoLayout.setOrientation(LinearLayout.VERTICAL);
        logoLayout.setGravity(Gravity.CENTER);
        logoLayout.setPadding(dp(10), dp(10), dp(10), dp(10));

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#1A1A1A"));
        gd.setStroke(dp(2), Color.parseColor("#2ECC71"));
        logoLayout.setBackground(gd);

        logoLayout.setClickable(true);
        logoLayout.setFocusable(true);

        TextView logoText = new TextView(this);
        logoText.setText("⚔️");
        logoText.setTextSize(28f);
        logoText.setPadding(dp(4), dp(4), dp(4), dp(4));
        logoLayout.addView(logoText);

        logoParams = new WindowManager.LayoutParams(
                dp(72), dp(72),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        logoParams.gravity = Gravity.TOP | Gravity.START;
        logoParams.x = prefs.getInt("logo_x", dp(50));
        logoParams.y = prefs.getInt("logo_y", dp(200));

        windowManager.addView(logoLayout, logoParams);
        logoView = logoLayout;

        logoLayout.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        if (Math.abs(dx) > dp(10) || Math.abs(dy) > dp(10)) {
                            isDragging = true;
                            logoParams.x += (int) dx;
                            logoParams.y += (int) dy;
                            startX = event.getRawX();
                            startY = event.getRawY();
                            windowManager.updateViewLayout(logoView, logoParams);
                            prefs.edit().putInt("logo_x", logoParams.x).putInt("logo_y", logoParams.y).apply();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleMenu() {
        if (isMenuOpen) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        if (menuView != null) {
            menuView.setVisibility(View.VISIBLE);
            menuView.animate().alpha(1.0f).setDuration(250)
                    .setInterpolator(new DecelerateInterpolator()).start();
            isMenuOpen = true;
        }
    }

    private void hideMenu() {
        if (menuView != null) {
            menuView.animate().alpha(0.0f).setDuration(200).withEndAction(() -> {
                menuView.setVisibility(View.GONE);
                isMenuOpen = false;
            }).start();
        }
    }

    private void createMenu() {
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(dp(20), dp(20), dp(20), dp(20));
        menuLayout.setGravity(Gravity.CENTER);

        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setShape(GradientDrawable.RECTANGLE);
        menuBg.setCornerRadius(dp(20));
        menuBg.setColor(Color.parseColor("#CC1A1A1A"));
        menuBg.setStroke(dp(1), Color.parseColor("#2ECC71"));
        menuLayout.setBackground(menuBg);

        TextView title = new TextView(this);
        title.setText("⚔️ NOBIAM MENU");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20f);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(16));
        menuLayout.addView(title);

        Button btnAdd = createStyledButton("➕ Добавить макрос", "#2ECC71");
        btnAdd.setOnClickListener(v -> {
            if (isSettingTarget) {
                Toast.makeText(this, "Уже настраиваете позицию", Toast.LENGTH_SHORT).show();
                return;
            }
            showTargetCircle();
        });
        menuLayout.addView(btnAdd);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackgroundColor(Color.parseColor("#333333"));
        divider.setPadding(0, dp(12), 0, dp(12));
        menuLayout.addView(divider);

        Button btnClose = createStyledButton("✕ Закрыть", "#E74C3C");
        btnClose.setOnClickListener(v -> hideMenu());
        menuLayout.addView(btnClose);

        menuParams = new WindowManager.LayoutParams(
                dp(360), WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.CENTER;
        menuParams.x = 0;
        menuParams.y = 0;
        menuParams.alpha = 0.0f;

        windowManager.addView(menuLayout, menuParams);
        menuView = menuLayout;

        // FIX: return false в ACTION_DOWN — чтобы кнопки работали
        menuLayout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = menuParams.x;
                        initialY = menuParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false; // 🔥 НЕ БЛОКИРУЕМ КНОПКИ

                    case MotionEvent.ACTION_MOVE:
                        menuParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        menuParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(menuView, menuParams);
                        return true;
                }
                return false;
            }
        });
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor(color));
        btn.setPadding(dp(20), dp(14), dp(20), dp(14));
        btn.setAllCaps(false);
        btn.setTextSize(16f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        btn.setLayoutParams(params);
        return btn;
    }

    private void showTargetCircle() {
        isSettingTarget = true;
        hideMenu();

        LinearLayout circleLayout = new LinearLayout(this);
        circleLayout.setGravity(Gravity.CENTER);
        circleLayout.setPadding(dp(8), dp(8), dp(8), dp(8));

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setStroke(dp(3), Color.parseColor("#FF4444"));
        circleBg.setColor(Color.parseColor("#33FF4444"));
        circleLayout.setBackground(circleBg);

        TextView hint = new TextView(this);
        hint.setText("🎯");
        hint.setTextSize(32f);
        circleLayout.addView(hint);

        int savedCircleX = prefs.getInt("circle_x", dp(100));
        int savedCircleY = prefs.getInt("circle_y", dp(300));

        circleParams = new WindowManager.LayoutParams(
                dp(120), dp(120),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        circleParams.gravity = Gravity.TOP | Gravity.START;
        circleParams.x = savedCircleX;
        circleParams.y = savedCircleY;

        windowManager.addView(circleLayout, circleParams);
        circleView = circleLayout;

        circleLayout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = circleParams.x;
                        initialY = circleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        circleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        circleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(circleView, circleParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        prefs.edit()
                                .putInt("circle_x", circleParams.x)
                                .putInt("circle_y", circleParams.y)
                                .apply();
                        targetX = circleParams.x + dp(60);
                        targetY = circleParams.y + dp(60);
                        prefs.edit()
                                .putInt("target_x", targetX)
                                .putInt("target_y", targetY)
                                .putBoolean("has_macro", true)
                                .apply();
                        finishTargetSetup();
                        return true;
                }
                return false;
            }
        });
    }

    private void finishTargetSetup() {
        isSettingTarget = false;
        if (circleView != null) {
            windowManager.removeView(circleView);
            circleView = null;
        }
        createMacroButton();
        showMenu();
    }

    private void createMacroButton() {
        if (macroButton != null) {
            try {
                windowManager.removeView(macroButton);
            } catch (Exception e) {}
        }

        macroButton = new Button(this);
        macroButton.setText("⚔️");
        macroButton.setTextSize(28f);
        macroButton.setAllCaps(false);
        macroButton.setPadding(0, 0, 0, 0);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#2ECC71"));
        macroButton.setBackground(gd);

        int savedX = prefs.getInt("macro_x", dp(100));
        int savedY = prefs.getInt("macro_y", dp(500));

        WindowManager.LayoutParams btnParams = new WindowManager.LayoutParams(
                dp(120), dp(120),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = savedX;
        btnParams.y = savedY;

        windowManager.addView(macroButton, btnParams);

        macroButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = btnParams.x;
                        initialY = btnParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        startClicking();
                        macroButton.setBackgroundColor(Color.parseColor("#FF4444"));
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > dp(10) || Math.abs(dy) > dp(10)) {
                            isDragging = true;
                            stopClicking();
                            btnParams.x = initialX + (int) dx;
                            btnParams.y = initialY + (int) dy;
                            windowManager.updateViewLayout(macroButton, btnParams);
                            prefs.edit().putInt("macro_x", btnParams.x).putInt("macro_y", btnParams.y).apply();
                            macroButton.setBackgroundColor(Color.parseColor("#2ECC71"));
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            stopClicking();
                        }
                        macroButton.setBackgroundColor(Color.parseColor("#2ECC71"));
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        stopClicking();
                        macroButton.setBackgroundColor(Color.parseColor("#2ECC71"));
                        return true;
                }
                return false;
            }
        });
    }

    private void startClicking() {
        ClickerService clickerService = ClickerService.getInstance();
        if (clickerService != null) {
            clickerService.setClickPosition(targetX, targetY);
            int cps = prefs.getInt("cps", 10);
            clickerService.setCPS(cps);
            clickerService.startClicking();
        } else {
            Toast.makeText(this, "⚠️ Включите специальные возможности", Toast.LENGTH_LONG).show();
        }
    }

    private void stopClicking() {
        ClickerService clickerService = ClickerService.getInstance();
        if (clickerService != null) {
            clickerService.stopClicking();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopClicking();
        try {
            if (logoView != null) windowManager.removeView(logoView);
            if (menuView != null) windowManager.removeView(menuView);
            if (circleView != null) windowManager.removeView(circleView);
            if (macroButton != null) windowManager.removeView(macroButton);
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
