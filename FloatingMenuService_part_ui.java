private void createFloatingIcon() {
    iconView = new ImageView(this);
    iconView.setImageResource(android.R.drawable.ic_input_add);
    iconView.setBackgroundResource(R.drawable.bg_circle);
    iconView.setPadding(20,20,20,20);

    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            150,
            150,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    params.gravity = Gravity.TOP | Gravity.START;
    params.x = 100;
    params.y = 300;

    windowManager.addView(iconView, params);

    iconView.setOnTouchListener(new View.OnTouchListener() {
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
                    params.x = initialX + (int)(event.getRawX() - initialTouchX);
                    params.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(iconView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    openMenu();
                    return true;
            }
            return false;
        }
    });
}
