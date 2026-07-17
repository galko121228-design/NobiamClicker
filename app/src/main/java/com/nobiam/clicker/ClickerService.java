package com.nobiam.clicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class ClickerService extends AccessibilityService {

    private static ClickerService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public static ClickerService get() {
        return instance;
    }

    public void click(int x, int y) {
        if (instance == null) return;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
