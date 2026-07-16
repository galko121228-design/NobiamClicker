package com.nobiam.clicker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private Button floatingButton;
    private WindowManager.LayoutParams params;
    private SharedPreferences prefs;
    private boolean isActive = false;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    private static final String PREF_NAME = "overlay_prefs";
    private static final String KEY_POS_X = "pos_x";
    private static final String KEY_POS_Y = "pos_y";
    private static final String KEY_ACTIVE = "clicker_active";

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        isActive = prefs.getBoolean(KEY_ACTIVE, false);
        int savedX = prefs.getInt(KEY_POS_X, 200);
        int savedY = prefs.getInt(KEY_POS_Y, 400);

        createFloatingView(savedX, savedY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createFloatingView(int savedX, int savedY) {
        // Создаём простую кнопку вместо кастомного View
        floatingButton = new Button(this);
        floatingButton.setText(isActive ? "⏸" : "▶");
        floatingButton.setTextSize(24f);
        floatingButton.setAllCaps(false);
        
        // Цвета
        int bgColor = isActive ? 0xFFFF4444 : 0xFF2ECC71;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(bgColor);
        floatingButton.setBackground(gd);
        floatingButton.setTextColor(0xFFFFFFFF);
        floatingButton.setPadding(0, 0, 0, 0);

        // Настройка параметров окна
        params = new WindowManager.LayoutParams(
                140, 140,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = savedX;
        params.y = savedY;

        // Добавляем View
        try {
            if (floatingButton.getParent() != null) {
                windowManager.removeView(floatingButton);
            }
            windowManager.addView(floatingButton, params);
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return;
        }

        // Обработка касаний (перетаскивание и нажатие)
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true;
                            params.x = initialX + (int) deltaX;
                            params.y = initialY + (int) deltaY;
                            try {
                                windowManager.updateViewLayout(floatingButton, params);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Сохраняем позицию
                        prefs.edit().putInt(KEY_POS_X, params.x).putInt(KEY_POS_Y, params.y).apply();
                        if (!isDragging) {
                            toggleClicker();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleClicker() {
        isActive = !isActive;
        prefs.edit().putBoolean(KEY_ACTIVE, isActive).apply();

        // Меняем внешний вид
        if (isActive) {
            floatingButton.setText("⏸");
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(0xFFFF4444);
            floatingButton.setBackground(gd);
        } else {
            floatingButton.setText("▶");
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(0xFF2ECC71);
            floatingButton.setBackground(gd);
        }

        // Подключаемся к ClickerService
        ClickerService clickerService = ClickerService.getInstance();
        if (clickerService != null) {
            if (isActive) {
                clickerService.startClicking();
                Toast.makeText(this, "Кликер ВКЛЮЧЁН", Toast.LENGTH_SHORT).show();
            } else {
                clickerService.stopClicking();
                Toast.makeText(this, "Кликер ВЫКЛЮЧЁН", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "⚠️ Включите доступность в настройках", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingButton != null && floatingButton.getParent() != null) {
                windowManager.removeView(floatingButton);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
