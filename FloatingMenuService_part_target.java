private void showTarget() {
    targetView = new View(this);
    targetView.setBackgroundResource(R.drawable.bg_target);

    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            200,
            200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    params.gravity = Gravity.TOP | Gravity.START;

    windowManager.addView(targetView, params);

    targetView.setOnTouchListener(new View.OnTouchListener() {
        int x,y;
        float tx,ty;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = params.x;
                    y = params.y;
                    tx = e.getRawX();
                    ty = e.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = x + (int)(e.getRawX() - tx);
                    params.y = y + (int)(e.getRawY() - ty);
                    windowManager.updateViewLayout(targetView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    saveCoords(params.x, params.y);
                    windowManager.removeView(targetView);
                    showTrigger();
                    return true;
            }
            return false;
        }
    });
}
