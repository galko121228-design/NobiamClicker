private void showTrigger() {
    triggerView = new View(this);
    triggerView.setBackgroundResource(R.drawable.bg_trigger);

    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            180,
            180,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
    );

    params.gravity = Gravity.TOP | Gravity.START;

    windowManager.addView(triggerView, params);

    triggerView.setOnTouchListener(new View.OnTouchListener() {

        boolean clicking = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    clicking = true;
                    startClicking();
                    return true;

                case MotionEvent.ACTION_UP:
                    clicking = false;
                    stopClicking();
                    return true;
            }
            return false;
        }
    });
}
