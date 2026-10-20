package com.astraflow.Chizuru.ui.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.FrameLayout;

final class GlassHostLayout extends FrameLayout {

    interface GlassTuner {
        void onSize(int w, int h, float cornerRadius);

        void onTheme(boolean dark);
    }

    interface DragHandler {
        boolean onIntercept(MotionEvent ev);

        boolean onTouch(MotionEvent ev);
    }

    private final float mDensity;
    private GlassTuner mTuner;
    private DragHandler mDragHandler;

    private int mShadowPad;
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mShadowClip = new Path();
    private int mShadowClipW = -1;
    private int mShadowClipH = -1;

    private final int mTouchSlop;
    private float mGestureDownX;
    private float mGestureDownY;
    private boolean mAncestorsBlocked;

    GlassHostLayout(Context context) {
        super(context);
        mDensity = context.getResources().getDisplayMetrics().density;
        mTouchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        setWillNotDraw(false);
    }

    void setupShadow(float density, boolean night) {
        mShadowPad = Math.round(density * 10f);
        setPadding(mShadowPad, mShadowPad, mShadowPad, mShadowPad);
        mShadowPaint.setColor(0xFF000000);
        mShadowPaint.setShadowLayer(density * 8f, 0f, density * 1.5f,
                night ? 0x33000000 : 0x1A000000);
        invalidate();
    }

    int shadowPad() {
        return mShadowPad;
    }

    void setGlassTuner(GlassTuner tuner) {
        mTuner = tuner;
        if (tuner != null) {
            tuner.onTheme(isNight(getContext()));
        }
    }

    private static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    void setDragHandler(DragHandler handler) {
        mDragHandler = handler;
    }

    private void protectGestureFromAncestors(MotionEvent ev) {
        int action = ev.getActionMasked();
        ViewParent parent = getParent();
        if (parent == null) {
            return;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            mGestureDownX = ev.getX();
            mGestureDownY = ev.getY();
            mAncestorsBlocked = true;
            parent.requestDisallowInterceptTouchEvent(true);
            return;
        }
        if (action == MotionEvent.ACTION_MOVE && mAncestorsBlocked) {
            float dx = ev.getX() - mGestureDownX;
            float dy = ev.getY() - mGestureDownY;
            if (Math.abs(dy) > mTouchSlop * 2f && Math.abs(dy) > Math.abs(dx)) {
                mAncestorsBlocked = false;
                parent.requestDisallowInterceptTouchEvent(false);
            }
            return;
        }
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                && mAncestorsBlocked) {
            mAncestorsBlocked = false;
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        protectGestureFromAncestors(ev);
        try {
            if (mDragHandler != null && mDragHandler.onIntercept(ev)) {
                return true;
            }
        } catch (Throwable t) {
            GlassLog.err("drag intercept failed", t);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        protectGestureFromAncestors(ev);
        try {
            if (mDragHandler != null && mDragHandler.onTouch(ev)) {
                return true;
            }
        } catch (Throwable t) {
            GlassLog.err("drag touch failed", t);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mTuner != null) {
            float radius = Math.min((h - mShadowPad * 2) * 0.5f, 30f * mDensity);
            mTuner.onSize(w - mShadowPad * 2, h - mShadowPad * 2, radius);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPillShadow(canvas);
    }

    private void drawPillShadow(Canvas canvas) {
        if (mShadowPad <= 0) {
            return;
        }
        float l = mShadowPad;
        float t = mShadowPad;
        float r = getWidth() - mShadowPad;
        float b = getHeight() - mShadowPad;
        if (r <= l || b <= t) {
            return;
        }
        float radius = (b - t) * 0.5f;
        if (mShadowClipW != getWidth() || mShadowClipH != getHeight()) {
            mShadowClip.reset();
            mShadowClip.addRoundRect(l, t, r, b, radius, radius, Path.Direction.CW);
            mShadowClipW = getWidth();
            mShadowClipH = getHeight();
        }
        int save = canvas.save();
        canvas.clipOutPath(mShadowClip);
        canvas.drawRoundRect(l, t, r, b, radius, radius, mShadowPaint);
        canvas.restoreToCount(save);
    }

    void reveal() {
        try {
            setPivotX(getWidth() * 0.5f);
            setPivotY(getHeight());
            setScaleY(0.86f);
            setAlpha(0f);
            animate().alpha(1f).scaleY(1f)
                    .setDuration(320L)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .start();
        } catch (Throwable t) {
            GlassLog.err("reveal failed", t);
        }
    }
}
