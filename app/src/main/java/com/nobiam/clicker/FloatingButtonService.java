package com.nobiam.clicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.WindowManager.LayoutParams;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private LinearLayout floatingView;
    private Button clickButton;
    private ClickerService clickerService;
    private boolean isActive = false;
    private int cps = 10;
    private Handler handler = new Handler();

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

        floatingView = new LinearLayout(this);
        floatingView.setBackgroundColor(0xCC1A1A1A);
        floatingView.setOrientation(LinearLayout.VERTICAL);
        floatingView.setPadding(16, 16, 16, 16);

        clickButton = new Button(this);
        clickButton.setText("▶");
        clickButton.setTextSize(24f);
        clickButton.setBackgroundColor(0xFF00e5ff);
        clickButton.setTextColor(0xFF000000);

        LayoutParams params = new LayoutParams(
                150, 150,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        LayoutParams.TYPE_APPLICATION_OVERLAY :
                        LayoutParams.TYPE_PHONE,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

        floatingView.addView(clickButton);
        windowManager.addView(floatingView, params);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
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
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getRawX() - initialTouchX);
                        float diffY = Math.abs(event.getRawY() - initialTouchY);
                        if (diffX < 10 && diffY < 10) {
                            toggleClicker();
                        }
                        return true;
                }
                return false;
            }
        });

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
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
