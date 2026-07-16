package com.nobiam.clicker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private SharedPreferences prefs;
    private View menuView;
    private View logoView; // Иконка-логотип
    private FrameLayout targetCircle;
    private Button macroButton;
    private boolean isSettingTarget = false;
    private int targetX = 500, targetY = 800;
    private boolean isHolding = false;
    private boolean isMenuOpen = false;

    private WindowManager.LayoutParams logoParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams circleParams;
    private WindowManager.LayoutParams buttonParams;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        targetX = prefs.getInt("target_x", 500);
        targetY = prefs.getInt("target_y", 800);

        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "⚠️ Включите специальные возможности", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        // Создаём иконку-логотип
        createLogo();
        // Если есть сохранённый макрос — создаём кнопку
        if (prefs.getBoolean("has_macro", false)) {
            createMacroButton();
        }
        // Создаём меню (но пока скрыто)
        createMenu();
        hideMenu();
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(getPackageName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    // ============================================================
    // 1. ИКОНКА-ЛОГОТИП (всегда висит поверх)
    // ============================================================
    private void createLogo() {
        int savedX = prefs.getInt("logo_x", 100);
        int savedY = prefs.getInt("logo_y", 300);

        LinearLayout logoLayout = new LinearLayout(this);
        logoLayout.setOrientation(LinearLayout.VERTICAL);
        logoLayout.setGravity(Gravity.CENTER);
        logoLayout.setPadding(16, 16, 16, 16);

        // Круглый фон логотипа
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#1A1A1A"));
        gd.setStroke(3, Color.parseColor("#2ECC71"));
        logoLayout.setBackground(gd);

        // Текст логотипа (можно заменить на иконку)
        TextView logoText = new TextView(this);
        logoText.setText("⚔️");
        logoText.setTextSize(28f);
        logoText.setPadding(16, 16, 16, 16);
        logoLayout.addView(logoText);

        logoParams = new WindowManager.LayoutParams(
                80, 80,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        logoParams.gravity = Gravity.TOP | Gravity.START;
        logoParams.x = savedX;
        logoParams.y = savedY;

        windowManager.addView(logoLayout, logoParams);
        logoView = logoLayout;

        // Перетаскивание логотипа
        logoLayout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = logoParams.x;
                        initialY = logoParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                            logoParams.x = initialX + (int) dx;
                            logoParams.y = initialY + (int) dy;
                            windowManager.updateViewLayout(logoView, logoParams);
                            // Сохраняем позицию логотипа
                            prefs.edit().putInt("logo_x", logoParams.x).putInt("logo_y", logoParams.y).apply();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            // Обычный клик — открываем/закрываем меню
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // ============================================================
    // 2. МЕНЮ (появляется при клике на логотип)
    // ============================================================
    private void createMenu() {
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(24, 24, 24, 24);
        menuLayout.setBackgroundColor(0xCC1A1A1A);
        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.RECTANGLE);
        border.setCornerRadius(20);
        border.setStroke(2, Color.parseColor("#2ECC71"));
        menuLayout.setBackground(border);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("⚔️ NOBIAM MENU");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setPadding(0, 0, 0, 16);
        menuLayout.addView(title);

        // Кнопка "Добавить макрос"
        Button btnAddMacro = new Button(this);
        btnAddMacro.setText("➕ Добавить макрос");
        btnAddMacro.setTextColor(Color.WHITE);
        btnAddMacro.setBackgroundColor(Color.parseColor("#2ECC71"));
        btnAddMacro.setPadding(30, 20, 30, 20);
        btnAddMacro.setOnClickListener(v -> {
            if (isSettingTarget) {
                Toast.makeText(this, "Уже настраиваете позицию", Toast.LENGTH_SHORT).show();
                return;
            }
            showTargetCircle();
        });
        menuLayout.addView(btnAddMacro);

        // Разделитель
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#333333"));
        divider.setPadding(0, 16, 0, 16);
        menuLayout.addView(divider);

        // Кнопка "Закрыть меню"
        Button btnCloseMenu = new Button(this);
        btnCloseMenu.setText("✕ Закрыть");
        btnCloseMenu.setTextColor(Color.WHITE);
        btnCloseMenu.setBackgroundColor(Color.parseColor("#E74C3C"));
        btnCloseMenu.setPadding(30, 20, 30, 20);
        btnCloseMenu.setOnClickListener(v -> hideMenu());
        menuLayout.addView(btnCloseMenu);

        menuParams = new WindowManager.LayoutParams(
                400, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.CENTER;
        menuParams.x = 0;
        menuParams.y = 0;

        windowManager.addView(menuLayout, menuParams);
        menuView = menuLayout;

        // Перетаскивание меню
        menuLayout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = menuParams.x;
                        initialY = menuParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        menuParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        menuParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(menuView, menuParams);
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleMenu() {
        if (isMenuOpen) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        if (menuView != null) {
            menuView.setVisibility(View.VISIBLE);
            isMenuOpen = true;
        }
    }

    private void hideMenu() {
        if (menuView != null) {
            menuView.setVisibility(View.GONE);
            isMenuOpen = false;
        }
    }

    // ============================================================
    // 3. КРУГЛЕШОК ДЛЯ НАСТРОЙКИ КООРДИНАТ
    // ============================================================
    private void showTargetCircle() {
        isSettingTarget = true;
        hideMenu(); // Скрываем меню, чтобы не мешало

        targetCircle = new FrameLayout(this);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setStroke(4, Color.parseColor("#FF4444"));
        circleBg.setColor(0x33FF4444);
        targetCircle.setBackground(circleBg);

        // Добавляем текст "СЮДА"
        TextView hint = new TextView(this);
        hint.setText("⬇️");
        hint.setTextSize(20f);
        hint.setGravity(Gravity.CENTER);
        targetCircle.addView(hint);

        circleParams = new WindowManager.LayoutParams(
                120, 120,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        circleParams.gravity = Gravity.TOP | Gravity.START;
        circleParams.x = targetX - 60;
        circleParams.y = targetY - 60;

        windowManager.addView(targetCircle, circleParams);

        targetCircle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = circleParams.x;
                        initialY = circleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        circleParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        circleParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(targetCircle, circleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        targetX = circleParams.x + 60;
                        targetY = circleParams.y + 60;
                        prefs.edit()
                            .putInt("target_x", targetX)
                            .putInt("target_y", targetY)
                            .putBoolean("has_macro", true)
                            .apply();
                        Toast.makeText(FloatingMenuService.this, "✅ Координаты сохранены", Toast.LENGTH_SHORT).show();
                        finishTargetSetup();
                        return true;
                }
                return false;
            }
        });
    }

    private void finishTargetSetup() {
        isSettingTarget = false;
        if (targetCircle != null) {
            windowManager.removeView(targetCircle);
            targetCircle = null;
        }
        createMacroButton();
        showMenu(); // Показываем меню обратно
    }

    // ============================================================
    // 4. КНОПКА-МАКРОС
    // ============================================================
    private void createMacroButton() {
        if (macroButton != null) {
            try {
                windowManager.removeView(macroButton);
            } catch (Exception e) {}
        }

        macroButton = new Button(this);
        macroButton.setText("⚔️");
        macroButton.setTextSize(28f);
        macroButton.setAllCaps(false);
        macroButton.setPadding(0, 0, 0, 0);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#2ECC71"));
        macroButton.setBackground(gd);

        int savedX = prefs.getInt("button_x", 200);
        int savedY = prefs.getInt("button_y", 500);

        buttonParams = new WindowManager.LayoutParams(
                120, 120,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = savedX;
        buttonParams.y = savedY;

        windowManager.addView(macroButton, buttonParams);

        macroButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = buttonParams.x;
                        initialY = buttonParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        isHolding = true;
                        startClicking();
                        macroButton.setBackgroundColor(Color.parseColor("#FF4444"));
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                            isDragging = true;
                            isHolding = false;
                            stopClicking();
                            buttonParams.x = initialX + (int) dx;
                            buttonParams.y = initialY + (int) dy;
                            windowManager.updateViewLayout(macroButton, buttonParams);
                            prefs.edit().putInt("button_x", buttonParams.x).putInt("button_y", buttonParams.y).apply();
                            macroButton.setBackgroundColor(Color.parseColor("#2ECC71"));
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        isHolding = false;
                        stopClicking();
                        macroButton.setBackgroundColor(Color.parseColor("#2ECC71"));
                        return true;
                }
                return false;
            }
        });
    }

    private void startClicking() {
        ClickerService clickerService = ClickerService.getInstance();
        if (clickerService != null) {
            clickerService.setClickPosition(targetX, targetY);
            int cps = prefs.getInt("cps", 10);
            clickerService.setCPS(cps);
            clickerService.startClicking();
        } else {
            Toast.makeText(this, "⚠️ Включите специальные возможности", Toast.LENGTH_LONG).show();
        }
    }

    private void stopClicking() {
        ClickerService clickerService = ClickerService.getInstance();
        if (clickerService != null) {
            clickerService.stopClicking();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopClicking();
        try {
            if (logoView != null) windowManager.removeView(logoView);
            if (menuView != null) windowManager.removeView(menuView);
            if (targetCircle != null) windowManager.removeView(targetCircle);
            if (macroButton != null) windowManager.removeView(macroButton);
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
