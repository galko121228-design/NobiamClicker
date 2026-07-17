package com.nobiam.clicker;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private View triggerView;
    private WindowManager.LayoutParams params;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;

    private boolean isMoving = false;

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createTrigger();
        startForegroundService();
    }

    private void createTrigger() {
        triggerView = new View(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#AA00BCD4")); // твой стиль
        bg.setShape(GradientDrawable.OVAL);

        triggerView.setBackground(bg);

        int size = dp(64);

        params = new WindowManager.LayoutParams(
                size,
                size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 300;
        params.y = 600;

        windowManager.addView(triggerView, params);

        setupTouch();
    }

    private void setupTouch() {
        triggerView.setOnTouchListener(new View.OnTouchListener() {
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        isMoving = false;

                        initialX = params.x;
                        initialY = params.y;

                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        downTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;

                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoving = true;
                        }

                        params.x = (int) (initialX + dx);
                        params.y = (int) (initialY + dy);

                        windowManager.updateViewLayout(triggerView, params);
                        return true;

                    case MotionEvent.ACTION_UP:

                        if (!isMoving) {
                            onTriggerClick();
                        }

                        return true;
                }

                return false;
            }
        });
    }

    private void onTriggerClick() {
        Toast.makeText(this, "Открыть меню (дальше добавим)", Toast.LENGTH_SHORT).show();
    }

    private void startForegroundService() {
        String channelId = "clicker_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Clicker Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Clicker запущен")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();

        startForeground(1, notification);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
