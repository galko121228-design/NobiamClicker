package com.nobiam.clicker;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

public class FloatingButtonService extends Service {

    private static final String TAG = "FloatingButtonService";
    private WindowManager windowManager;
    private View floatingView;
    private ImageView iconView;
    private WindowManager.LayoutParams params;
    private SharedPreferences prefs;
    private boolean isActive = false;
    private boolean isMoving = false;
    private int initialX, initialY, initialTouchX, initialTouchY;

    private static final String PREF_NAME = "overlay_prefs";
    private static final String KEY_POS_X = "pos_x";
    private static final String KEY_POS_Y = "pos_y";
    private static final String KEY_ACTIVE = "clicker_active";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            isActive = prefs.getBoolean(KEY_ACTIVE, false);
            int savedX = prefs.getInt(KEY_POS_X, 100);
            int savedY = prefs.getInt(KEY_POS_Y, 200);

            floatingView = createFloatingView();

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = savedX;
            params.y = savedY;
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Нет разрешения на показ поверх экрана", Toast.LENGTH_SHORT).show();
                stopSelf();
                return START_NOT_STICKY;
            }

            if (floatingView == null) {
                floatingView = createFloatingView();
            }

            // Безопасное добавление View
            if (floatingView != null && windowManager != null) {
                if (floatingView.getParent() != null) {
                    try {
                        windowManager.removeView(floatingView);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to remove view: " + e.getMessage());
                    }
                }
                windowManager.addView(floatingView, params);
            }

            updateButtonAppearance(false);
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand error: " + e.getMessage());
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        return START_STICKY;
    }

    private View createFloatingView() {
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xE8101A14);
            bg.setStroke(2, isActive ? 0xFF2ECC71 : 0xFFE74C3C);
            bg.setAlpha(230);

            iconView = new ImageView(this);
            iconView.setLayoutParams(new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            ));
            iconView.setPadding(18, 18, 18, 18);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setColorFilter(0xFFFFFFFF);

            // Загрузка иконки через AppCompatResources для совместимости
            int drawableRes = isActive ? R.drawable.avd_pause_to_play : R.drawable.avd_play_to_pause;
            Drawable drawable = AppCompatResources.getDrawable(this, drawableRes);
            if (drawable != null) {
                iconView.setImageDrawable(drawable);
            }

            floatingView = new View(this);
            floatingView.setLayoutParams(new WindowManager.LayoutParams(dpToPx(60), dpToPx(60)));
            floatingView.setBackground(bg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                floatingView.setElevation(dpToPx(isActive ? 12 : 6));
            }
            floatingView.setAlpha(isActive ? 1.0f : 0.6f);

            floatingView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = (int) event.getRawX();
                            initialTouchY = (int) event.getRawY();
                            isMoving = false;
                            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int deltaX = (int) event.getRawX() - initialTouchX;
                            int deltaY = (int) event.getRawY() - initialTouchY;
                            if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                                isMoving = true;
                                params.x = initialX + deltaX;
                                params.y = initialY + deltaY;
                                windowManager.updateViewLayout(floatingView, params);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200)
                                    .setInterpolator(new OvershootInterpolator()).start();
                            if (!isMoving) {
                                toggleClicker();
                            }
                            prefs.edit().putInt(KEY_POS_X, params.x).putInt(KEY_POS_Y, params.y).apply();
                            return true;
                    }
                    return false;
                }
            });

            return floatingView;
        } catch (Exception e) {
            Log.e(TAG, "createFloatingView error: " + e.getMessage());
            return new View(this);
        }
    }

    private void toggleClicker() {
        try {
            isActive = !isActive;
            prefs.edit().putBoolean(KEY_ACTIVE, isActive).apply();

            ClickerService clickerService = ClickerService.getInstance();
            if (clickerService != null) {
                if (isActive) {
                    clickerService.startClicking();
                } else {
                    clickerService.stopClicking();
                }
            } else {
                Toast.makeText(this, "Сначала включите доступность в настройках", Toast.LENGTH_SHORT).show();
            }

            updateButtonAppearance(true);
        } catch (Exception e) {
            Log.e(TAG, "toggleClicker error: " + e.getMessage());
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonAppearance(boolean animate) {
        try {
            if (floatingView == null) return;

            GradientDrawable bg = (GradientDrawable) floatingView.getBackground();
            if (bg == null) return;

            int targetBorderColor = isActive ? 0xFF2ECC71 : 0xFFE74C3C;
            float targetAlpha = isActive ? 1.0f : 0.6f;

            // Обновляем иконку через AppCompatResources
            int drawableRes = isActive ? R.drawable.avd_play_to_pause : R.drawable.avd_pause_to_play;
            Drawable drawable = AppCompatResources.getDrawable(this, drawableRes);
            if (drawable != null) {
                if (drawable instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) drawable).start();
                }
                iconView.setImageDrawable(drawable);
            }

            if (animate) {
                ValueAnimator borderAnim = ValueAnimator.ofArgb(
                        isActive ? 0xFFE74C3C : 0xFF2ECC71, targetBorderColor);
                borderAnim.setDuration(250);
                borderAnim.addUpdateListener(animation -> {
                    bg.setStroke(2, (int) animation.getAnimatedValue());
                    floatingView.setBackground(bg);
                });
                borderAnim.start();

                floatingView.animate().alpha(targetAlpha).setDuration(300).start();
            } else {
                bg.setStroke(2, targetBorderColor);
                floatingView.setAlpha(targetAlpha);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateButtonAppearance error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            // Останавливаем анимацию, если она есть
            if (iconView != null && iconView.getDrawable() instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) iconView.getDrawable()).stop();
            }

            if (floatingView != null && floatingView.getParent() != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy error: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
