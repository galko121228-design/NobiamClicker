package com.nobiam.clicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private LinearLayout floatingView;
    private Button clickButton;
    private ClickerService clickerService;
    private boolean isActive = false;
    private int cps = 10;
    private Handler handler = new Handler();

    // Координаты для перетаскивания
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        cps = intent.getIntExtra("cps", 10);
        createFloatingView();
        return START_STICKY;
    }

    private void createFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Контейнер для кнопки
        floatingView = new LinearLayout(this);
        floatingView.setBackgroundColor(0xCC1A1A1A);
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setPadding(16, 16, 16, 16);

        // Сама кнопка
        clickButton = new Button(this);
        clickButton.setText("▶");
        clickButton.setTextSize(24f);
        clickButton.setBackgroundColor(0xFF00e5ff);
        clickButton.setTextColor(0xFF000000);

        // Параметры окна
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                150, 150,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

        floatingView.addView(clickButton);
        windowManager.addView(floatingView, params);

        // Обработчик касаний (перетаскивание + клик)
        floatingView.setOnTouchListener(new View.OnTouchListener() {
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
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // Это был клик, а не перетаскивание
                            toggleClicker();
                        }
                        return true;
                }
                return false;
            }
        });

        // Подключаемся к сервису доступности
        connectToAccessibilityService();
    }

    private void connectToAccessibilityService() {
        handler.postDelayed(() -> {
            clickerService = ClickerService.getInstance();
            if (clickerService != null) {
                clickerService.setCPS(cps);
                Toast.makeText(this, "Кликер готов", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Включите доступность в настройках", Toast.LENGTH_LONG).show();
            }
        }, 2000);
    }

    private void toggleClicker() {
        if (clickerService == null) {
            Toast.makeText(this, "Сначала включите доступность", Toast.LENGTH_SHORT).show();
            return;
        }

        isActive = !isActive;
        if (isActive) {
            clickerService.startClicking();
            clickButton.setText("⏸");
            clickButton.setBackgroundColor(0xFFFF4444);
        } else {
            clickerService.stopClicking();
            clickButton.setText("▶");
            clickButton.setBackgroundColor(0xFF00e5ff);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clickerService != null) {
            clickerService.stopClicking();
        }
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
}
