package com.nobiam.clicker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
    private float initialTouchX, initialTouchY;
    private int initialX, initialY;
    private boolean isDragging;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Простая кнопка
        floatingButton = new Button(this);
        floatingButton.setText("▶");
        floatingButton.setTextSize(20);
        floatingButton.setBackgroundColor(Color.parseColor("#2ECC71"));
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setAllCaps(false);

        int savedX = prefs.getInt("x", 200);
        int savedY = prefs.getInt("y", 400);

        params = new WindowManager.LayoutParams(
                120, 120,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = savedX;
        params.y = savedY;

        // Добавляем кнопку
        try {
            windowManager.addView(floatingButton, params);
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return;
        }

        // Обработка касаний
        floatingButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true;
                        params.x = initialX + (int) dx;
                        params.y = initialY + (int) dy;
                        windowManager.updateViewLayout(floatingButton, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    prefs.edit().putInt("x", params.x).putInt("y", params.y).apply();
                    if (!isDragging) {
                        toggleClicker();
                    }
                    return true;
            }
            return false;
        });
    }

    private void toggleClicker() {
        isActive = !isActive;
        if (isActive) {
            floatingButton.setText("⏸");
            floatingButton.setBackgroundColor(Color.parseColor("#E74C3C"));
            Toast.makeText(this, "▶ Кликер включён", Toast.LENGTH_SHORT).show();
            
            // Пробуем подключиться к ClickerService
            ClickerService cs = ClickerService.getInstance();
            if (cs != null) {
                cs.startClicking();
            } else {
                Toast.makeText(this, "⚠️ Включите доступность", Toast.LENGTH_LONG).show();
            }
        } else {
            floatingButton.setText("▶");
            floatingButton.setBackgroundColor(Color.parseColor("#2ECC71"));
            Toast.makeText(this, "⏸ Кликер выключен", Toast.LENGTH_SHORT).show();
            
            ClickerService cs = ClickerService.getInstance();
            if (cs != null) {
                cs.stopClicking();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingButton != null && windowManager != null) {
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
