package com.nobiam.clicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

public class ClickerService extends AccessibilityService {

    private static ClickerService instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isClicking = false;
    private int clickX = 500, clickY = 800;
    private int delayMs = 100;
    private static final int CLICK_DURATION = 50;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static ClickerService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        stopClicking();
    }

    public void setClickPosition(int x, int y) {
        this.clickX = x;
        this.clickY = y;
    }

    public void setCPS(int cps) {
        if (cps > 0) {
            this.delayMs = 1000 / cps;
        }
    }

    public void startClicking() {
        if (isClicking) return;
        isClicking = true;
        performClick();
    }

    public void stopClicking() {
        isClicking = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void performClick() {
        if (!isClicking) return;

        try {
            Path clickPath = new Path();
            clickPath.moveTo(clickX, clickY);

            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(clickPath, 0, CLICK_DURATION))
                    .build();

            dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    if (isClicking) {
                        handler.postDelayed(() -> performClick(), delayMs);
                    }
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    if (isClicking) {
                        handler.postDelayed(() -> performClick(), 50);
                    }
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
            if (isClicking) {
                handler.postDelayed(() -> performClick(), 100);
            }
        }
    }
}
