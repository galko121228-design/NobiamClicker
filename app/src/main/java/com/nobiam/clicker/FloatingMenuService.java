package com.nobiam.clicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private Button floatingButton;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingButton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createFloatingButton() {
        try {
            // Кнопка
            floatingButton = new Button(this);
            floatingButton.setText("⚔️");
            floatingButton.setTextSize(32f);
            floatingButton.setBackgroundColor(Color.parseColor("#2ECC71"));
            floatingButton.setTextColor(Color.WHITE);
            floatingButton.setAllCaps(false);

            // ============================================================
            // ГЛАВНОЕ: правильный тип окна для Android 8+
            // ============================================================
            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            params = new WindowManager.LayoutParams(
                    160, 160,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 200;
            params.y = 400;

            // Добавляем View
            windowManager.addView(floatingButton, params);
            Toast.makeText(this, "✅ Оверлей появился!", Toast.LENGTH_SHORT).show();

            // Перетаскивание
            floatingButton.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingButton, params);
                            return true;
                        case MotionEvent.ACTION_UP:
                            Toast.makeText(FloatingMenuService.this, "⏳ Кнопка работает!", Toast.LENGTH_SHORT).show();
                            return true;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingButton != null && windowManager != null) {
                windowManager.removeView(floatingButton);
            }
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
