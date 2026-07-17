package com.nobiam.clicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class FloatingMenuService extends AccessibilityService {

    private WindowManager windowManager;
    private View menuView;
    private View targetView;

    private int targetX = 500;
    private int targetY = 800;

    private boolean clicking = false;
    private Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showMenu();
    }

    private void showMenu() {
        Button btn = new Button(this);
        btn.setText("●");
        btn.setTextSize(18);
        btn.setBackgroundColor(Color.BLACK);
        btn.setTextColor(Color.WHITE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                180,
                180,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

        windowManager.addView(btn, params);
        menuView = btn;

        btn.setOnClickListener(v -> showTarget());

        btn.setOnLongClickListener(v -> {
            clicking = !clicking;
            if (clicking) startClicking();
            return true;
        });

        btn.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int) (event.getRawX() - touchX);
                        params.y = startY + (int) (event.getRawY() - touchY);
                        windowManager.updateViewLayout(menuView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void showTarget() {
        if (targetView != null) return;

        View target = new View(this);
        target.setBackgroundColor(Color.RED);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                80,
                80,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = targetX;
        params.y = targetY;

        windowManager.addView(target, params);
        targetView = target;

        target.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int) (event.getRawX() - touchX);
                        params.y = startY + (int) (event.getRawY() - touchY);

                        targetX = params.x;
                        targetY = params.y;

                        windowManager.updateViewLayout(targetView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void startClicking() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!clicking) return;

                Path path = new Path();
                path.moveTo(targetX + 40, targetY + 40);

                GestureDescription.Builder builder = new GestureDescription.Builder();
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));

                dispatchGesture(builder.build(), null, null);

                handler.postDelayed(this, 200);
            }
        }, 200);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (menuView != null) windowManager.removeView(menuView);
        if (targetView != null) windowManager.removeView(targetView);
    }
}
