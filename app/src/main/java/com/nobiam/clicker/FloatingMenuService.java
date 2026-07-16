package com.nobiam.clicker;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

import androidx.core.app.NotificationCompat;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private SharedPreferences prefs;

    private View logoView, menuView, circleView;
    private Button macroButton;

    private WindowManager.LayoutParams logoParams, menuParams, circleParams;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        startForeground(1, createNotification());

        createLogo();
        createMenu();
        hideMenu();

        if (prefs.getBoolean("has_macro", false)) {
            createMacroButton();
        }
    }

    private Notification createNotification() {
        String id = "overlay";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    id, "Overlay", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(ch);
        }

        return new NotificationCompat.Builder(this, id)
                .setContentTitle("Clicker")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .build();
    }

    private int dp(int v) {
        return (int)(v * density);
    }

    private void createLogo() {
        TextView logo = new TextView(this);
        logo.setText("⚔️");
        logo.setTextSize(28f);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.BLACK);
        logo.setClickable(true);

        logoParams = new WindowManager.LayoutParams(
                dp(70), dp(70),
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        logoParams.gravity = Gravity.TOP | Gravity.START;
        logoParams.x = 100;
        logoParams.y = 300;

        windowManager.addView(logo, logoParams);
        logoView = logo;

        logo.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY;
            boolean drag = false;

            public boolean onTouch(View v, MotionEvent e) {

                switch (e.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = e.getRawX();
                        startY = e.getRawY();
                        drag = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - startX;
                        float dy = e.getRawY() - startY;

                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            drag = true;

                            logoParams.x += dx;
                            logoParams.y += dy;

                            startX = e.getRawX();
                            startY = e.getRawY();

                            windowManager.updateViewLayout(logoView, logoParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!drag) toggleMenu();
                        return true;
                }
                return false;
            }
        });
    }

    private void createMenu() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));

        Button add = new Button(this);
        add.setText("Добавить точку");
        add.setOnClickListener(v -> showCircle());

        Button close = new Button(this);
        close.setText("Закрыть");
        close.setOnClickListener(v -> hideMenu());

        layout.addView(add);
        layout.addView(close);

        menuParams = new WindowManager.LayoutParams(
                dp(250), WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        menuParams.gravity = Gravity.CENTER;

        windowManager.addView(layout, menuParams);
        menuView = layout;
    }

    private void toggleMenu() {
        if (isMenuOpen) hideMenu(); else showMenu();
    }

    private void showMenu() {
        menuView.setVisibility(View.VISIBLE);
        isMenuOpen = true;
    }

    private void hideMenu() {
        menuView.setVisibility(View.GONE);
        isMenuOpen = false;
    }

    private void showCircle() {
        isSettingTarget = true;
        hideMenu();

        View circle = new View(this);
        circle.setBackgroundColor(Color.RED);

        circleParams = new WindowManager.LayoutParams(
                dp(100), dp(100),
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        circleParams.gravity = Gravity.TOP | Gravity.START;
        circleParams.x = 200;
        circleParams.y = 400;

        windowManager.addView(circle, circleParams);
        circleView = circle;

        circle.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;

            public boolean onTouch(View v, MotionEvent e) {

                switch (e.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX();
                        sy = e.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        circleParams.x += e.getRawX() - sx;
                        circleParams.y += e.getRawY() - sy;

                        sx = e.getRawX();
                        sy = e.getRawY();

                        windowManager.updateViewLayout(circleView, circleParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        targetX = circleParams.x;
                        targetY = circleParams.y;

                        prefs.edit()
                                .putInt("target_x", targetX)
                                .putInt("target_y", targetY)
                                .putBoolean("has_macro", true)
                                .apply();

                        windowManager.removeView(circleView);
                        circleView = null;

                        createMacroButton();
                        return true;
                }
                return false;
            }
        });
    }

    private void createMacroButton() {
        if (macroButton != null) return;

        macroButton = new Button(this);
        macroButton.setText("▶");

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                dp(70), dp(70),
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 300;
        p.y = 500;

        windowManager.addView(macroButton, p);

        macroButton.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent e) {

                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    Toast.makeText(FloatingMenuService.this,
                            "CLICK " + targetX + "," + targetY,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
