package com.nobiam.clicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.*;
import android.widget.ImageView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private View triggerView;
    private WindowManager.LayoutParams triggerParams;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createTrigger();
    }

    private void createTrigger() {

        ImageView trigger = new ImageView(this);

        // 🔥 КРУГЛАЯ КНОПКА (как ты хотел)
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF5722"));
        bg.setShape(GradientDrawable.OVAL);
        trigger.setBackground(bg);

        triggerView = trigger;

        triggerParams = new WindowManager.LayoutParams(
                140,
                140,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        triggerParams.gravity = Gravity.TOP | Gravity.START;
        triggerParams.x = 300;
        triggerParams.y = 600;

        windowManager.addView(triggerView, triggerParams);

        setupTouch();
    }

    private void setupTouch() {
        triggerView.setOnTouchListener(new View.OnTouchListener() {

            long pressTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        initialX = triggerParams.x;
                        initialY = triggerParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        pressTime = System.currentTimeMillis();

                        // 👉 СТАРТ КЛИКОВ (пока заглушка)
                        startClicking();

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        triggerParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        triggerParams.y = initialY + (int) (event.getRawY() - initialTouchY);

                        windowManager.updateViewLayout(triggerView, triggerParams);
                        return true;

                    case MotionEvent.ACTION_UP:

                        // 👉 СТОП КЛИКОВ
                        stopClicking();

                        // 👉 если это был клик (не перетаскивание)
                        if (System.currentTimeMillis() - pressTime < 200) {
                            // позже тут откроем меню
                        }

                        return true;
                }
                return false;
            }
        });
    }

    private void startClicking() {
        // пока пусто (добавим в части 3)
    }

    private void stopClicking() {
        // пока пусто
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

    private void showTarget() {

        if (targetView != null) return;

        ImageView target = new ImageView(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x55FF0000);
        bg.setShape(GradientDrawable.OVAL);
        bg.setStroke(4, 0xFFFF0000);
        target.setBackground(bg);

        targetView = target;

        targetParams = new WindowManager.LayoutParams(
                160,
                160,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        targetParams.gravity = Gravity.TOP | Gravity.START;
        targetParams.x = targetX;
        targetParams.y = targetY;

        windowManager.addView(targetView, targetParams);

        targetView.setOnTouchListener(new View.OnTouchListener() {

            int initX, initY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        initX = targetParams.x;
                        initY = targetParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        targetParams.x = initX + (int)(event.getRawX() - touchX);
                        targetParams.y = initY + (int)(event.getRawY() - touchY);

                        windowManager.updateViewLayout(targetView, targetParams);
                        return true;

                    case MotionEvent.ACTION_UP:

                        // 💾 СОХРАНЯЕМ КООРДИНАТЫ
                        targetX = targetParams.x;
                        targetY = targetParams.y;

                        getSharedPreferences("cfg", MODE_PRIVATE)
                                .edit()
                                .putInt("x", targetX)
                                .putInt("y", targetY)
                                .apply();

                        // ❌ УБИРАЕМ ПРИЦЕЛ
                        windowManager.removeView(targetView);
                        targetView = null;

                        return true;
                }

                return false;
            }
        });
    }


    private void startClicking() {
        if (clicking) return;

        clicking = true;

        clickThread = new Thread(() -> {

            while (clicking) {

                try {

                    int x = getSharedPreferences("cfg", MODE_PRIVATE).getInt("x", 500);
                    int y = getSharedPreferences("cfg", MODE_PRIVATE).getInt("y", 800);
                    int speed = getSharedPreferences("cfg", MODE_PRIVATE).getInt("speed", 5);

                    ClickerService svc = ClickerService.get();

                    if (svc != null) {
                        svc.click(x, y);
                    }

                    Thread.sleep(1000 / Math.max(1, speed));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        clickThread.start();
    }

    private void stopClicking() {
        clicking = false;
    }

