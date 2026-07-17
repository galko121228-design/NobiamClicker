package com.example.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.*;
import android.os.Build;
import android.view.*;
import android.widget.*;

public class OverlayService extends AccessibilityService {

    WindowManager wm;

    View trigger;
    View clickPoint;
    LineView line;

    WindowManager.LayoutParams triggerParams;
    WindowManager.LayoutParams clickParams;
    WindowManager.LayoutParams lineParams;

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        createTrigger();
        createClickPoint();
        createLine();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}


    private void createTrigger() {

        trigger = new View(this);
        trigger.setBackgroundColor(Color.parseColor("#AAFF8800"));

        triggerParams = new WindowManager.LayoutParams(
                200, 200,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        triggerParams.x = 300;
        triggerParams.y = 300;

        wm.addView(trigger, triggerParams);

        trigger.setOnTouchListener(new View.OnTouchListener() {

            int startX, startY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = triggerParams.x;
                        startY = triggerParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        triggerParams.x = startX + (int)(event.getRawX() - touchX);
                        triggerParams.y = startY + (int)(event.getRawY() - touchY);
                        wm.updateViewLayout(trigger, triggerParams);
                        updateLine();
                        return true;
                }

                return false;
            }
        });
    }


    private void createClickPoint() {

        clickPoint = new View(this);
        clickPoint.setBackgroundColor(Color.RED);

        clickParams = new WindowManager.LayoutParams(
                60, 60,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        clickParams.x = 600;
        clickParams.y = 600;

        wm.addView(clickPoint, clickParams);

        clickPoint.setOnTouchListener(new View.OnTouchListener() {

            int startX, startY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = clickParams.x;
                        startY = clickParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        clickParams.x = startX + (int)(event.getRawX() - touchX);
                        clickParams.y = startY + (int)(event.getRawY() - touchY);
                        wm.updateViewLayout(clickPoint, clickParams);
                        updateLine();
                        return true;

                    case MotionEvent.ACTION_UP:
                        performClick(clickParams.x, clickParams.y);
                        return true;
                }

                return false;
            }
        });
    }


    private void createLine() {

        line = new LineView(this);

        lineParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );

        wm.addView(line, lineParams);
    }

    private void updateLine() {
        line.setPoints(
                triggerParams.x + 100,
                triggerParams.y + 100,
                clickParams.x + 30,
                clickParams.y + 30
        );
    }


    private void performClick(int x, int y) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            Path path = new Path();
            path.moveTo(x, y);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));

            dispatchGesture(builder.build(), null, null);
        }
    }


    public static class LineView extends View {

        Paint paint = new Paint();
        int x1, y1, x2, y2;

        public LineView(android.content.Context context) {
            super(context);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(6f);
        }

        public void setPoints(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

}
