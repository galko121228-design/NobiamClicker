package com.nobiam.clicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.RequiresApi;

public class ClickerService extends AccessibilityService {

    private static ClickerService instance;
    private boolean isClicking = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int clickX = 0;
    private int clickY = 0;
    private int delayMs = 50; // по умолчанию 20 CPS

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopClicking();
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
        if (isClicking || (clickX == 0 && clickY == 0)) return;
        isClicking = true;
        performClick();
    }

    public void stopClicking() {
        isClicking = false;
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isClicking() {
        return isClicking;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void performClick() {
        if (!isClicking) return;

        // Создаём путь для клика
        Path clickPath = new Path();
        clickPath.moveTo(clickX, clickY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 1));

        // Отправляем жест
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                // Планируем следующий клик с задержкой
                if (isClicking) {
                    int variance = (int) (Math.random() * 10 - 5);
                    int nextDelay = Math.max(delayMs + variance, 30);
                    handler.postDelayed(() -> performClick(), nextDelay);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                // Если жест отменился — пробуем ещё раз
                if (isClicking) {
                    handler.postDelayed(() -> performClick(), 50);
                }
            }
        }, null);
    }
}
