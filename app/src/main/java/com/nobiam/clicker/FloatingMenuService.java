package com.nobiam.clicker;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "🔵 1. Сервис создан", Toast.LENGTH_SHORT).show();
        
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Toast.makeText(this, "🔵 2. WindowManager получен", Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "❌ Нет разрешения Overlay", Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
            Toast.makeText(this, "🔵 3. Overlay разрешение есть", Toast.LENGTH_SHORT).show();

            // Создаём простую кнопку
            Button button = new Button(this);
            button.setText("⚔️");
            button.setTextSize(32f);
            button.setBackgroundColor(Color.parseColor("#2ECC71"));
            button.setTextColor(Color.WHITE);
            button.setAllCaps(false);

            params = new WindowManager.LayoutParams(
                    160, 160,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 200;
            params.y = 400;

            Toast.makeText(this, "🔵 4. Параметры окна готовы", Toast.LENGTH_SHORT).show();
            
            windowManager.addView(button, params);
            floatingView = button;
            
            Toast.makeText(this, "✅ Оверлей появился! Ищите ⚔️", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (floatingView != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {}
        Toast.makeText(this, "🔴 Сервис остановлен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
