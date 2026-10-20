package com.astraflow.Chizuru.ui.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public final class GlassBottomBar {

    public interface OnTabSelected {
        void onTabSelected(int index);
    }

    private static final int BAR_HEIGHT_DP = 58;
    private static final int TAB_ICON_DP = 22;
    private static final int TAB_MIN_WIDTH_DP = 68;

    private final Context mContext;
    private final float mDensity;
    private final boolean mNight;
    private final GlassHostLayout mHost;
    private final LinearLayout mRow;
    private LiquidGlassPanel mPanel;
    private DropletPanel mDroplet;
    private DropletDragController mDrag;

    private final int[] mPos = new int[2];
    private final ArrayList<View> mTabs = new ArrayList<>();
    private final ArrayList<ImageView> mIcons = new ArrayList<>();
    private final ArrayList<TextView> mLabels = new ArrayList<>();

    private OnTabSelected mListener;
    private int mSelected = -1;
    private boolean mPlaced;


    public GlassBottomBar(Context ctx, ViewGroup backdrop) {
        mContext = ctx;
        mDensity = ctx.getResources().getDisplayMetrics().density;
        mNight = (ctx.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        mHost = new GlassHostLayout(ctx);
        mHost.setupShadow(mDensity, mNight);
        mHost.setClipChildren(false);
        mHost.setClipToPadding(false);

        mRow = new LinearLayout(ctx);
        mRow.setOrientation(LinearLayout.HORIZONTAL);
        mRow.setGravity(Gravity.CENTER);
        mRow.setPadding(dp(6), 0, dp(6), 0);
        mHost.addView(mRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(BAR_HEIGHT_DP * mDensity)));

        attachRenderer(ctx, backdrop);
        mHost.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mTabs.isEmpty()) {
                            return;
                        }
                        syncDroplet();
                        if (!mPlaced) {
                            mPlaced = true;
                            if (mSelected < 0) {
                                select(0, false);
                            } else if (mDrag != null) {
                                mDrag.animateToIndex(mSelected, true);
                            }
                            mHost.reveal();
                        }
                    }
                });
    }

    private void attachRenderer(Context ctx, ViewGroup backdrop) {
        if (android.os.Build.VERSION.SDK_INT < LiquidGlassPanel.MIN_SDK) {
            return;
        }
        try {
            mPanel = new LiquidGlassPanel(ctx, backdrop, mDensity, mNight);
            mHost.addView(mPanel, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            mDroplet = new DropletPanel(ctx, backdrop, mRow, mDensity, mNight);
            mDroplet.setVisibility(View.INVISIBLE);
            mHost.addView(mDroplet, new FrameLayout.LayoutParams(0, 0,
                    Gravity.TOP | Gravity.START));
            mDroplet.setPill(mPanel);

            mDrag = new DropletDragController(mDroplet, mRow, mDensity, mNight);
            mDrag.setPill(mPanel);
            mDrag.setHost(mHost);
            mHost.setDragHandler(mDrag);

            mHost.setGlassTuner(new GlassHostLayout.GlassTuner() {
                @Override
                public void onSize(int w, int h, float cornerRadius) {
                    if (mPanel != null) {
                        mPanel.invalidate();
                    }
                }

                @Override
                public void onTheme(boolean dark) {
                    if (mPanel != null) {
                        mPanel.setTheme(dark);
                    }
                    if (mDroplet != null) {
                        mDroplet.setTheme(dark);
                    }
                }
            });
        } catch (Throwable t) {
            GlassLog.err("glass renderer unavailable", t);
        }
    }

    public View view() {
        return mHost;
    }

    public int shadowPad() {
        return mHost.shadowPad();
    }

    public void setTabs(String[] labels, int[] icons) {
        mRow.removeAllViews();
        mTabs.clear();
        mIcons.clear();
        mLabels.clear();
        for (int i = 0; i < labels.length; i++) {
            View tab = buildTab(labels[i], icons[i]);
            final int index = i;
            tab.setOnClickListener(v -> select(index, true));
            mRow.addView(tab, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mTabs.add(tab);
        }
        if (mDroplet != null) {
            mDroplet.setTabRow(mRow);
        }
        if (mDrag != null) {
            mDrag.setTabRow(mRow);
        }
        mSelected = -1;
    }

    private View buildTab(String label, int iconRes) {
        LinearLayout tab = new LinearLayout(mContext);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(true);
        tab.setFocusable(true);
        tab.setPadding(dp(10), dp(5), dp(10), dp(5));
        tab.setMinimumWidth(dp(TAB_MIN_WIDTH_DP));

        ImageView icon = new ImageView(mContext);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconLp =
                new LinearLayout.LayoutParams(dp(TAB_ICON_DP), dp(TAB_ICON_DP));
        tab.addView(icon, iconLp);

        TextView text = new TextView(mContext);
        text.setText(label);
        text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f);
        text.setIncludeFontPadding(false);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = dp(3);
        tab.addView(text, textLp);

        mIcons.add(icon);
        mLabels.add(text);
        paintTab(mIcons.size() - 1, false);
        return tab;
    }

    private void paintTab(int index, boolean selected) {
        int colour = selected
                ? (mNight ? 0xFF0091FF : 0xFF0088FF)
                : (mNight ? 0x99EBEBF5 : 0x993C3C43);
        ImageView icon = mIcons.get(index);
        icon.setImageTintList(ColorStateList.valueOf(colour));
        mLabels.get(index).setTextColor(colour);
    }

    public void setOnTabSelected(OnTabSelected listener) {
        mListener = listener;
    }

    public int selectedIndex() {
        return Math.max(0, mSelected);
    }

    public void setSelected(int index) {
        select(index, false);
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= mTabs.size()) {
            return;
        }
        int previous = mSelected;
        mSelected = index;
        for (int i = 0; i < mTabs.size(); i++) {
            boolean on = i == index;
            mTabs.get(i).setSelected(on);
            paintTab(i, on);
        }
        syncDroplet();
        if (mDrag != null) {
            mDrag.animateToIndex(index, previous < 0);
        }
        if (notify && mListener != null && previous != index) {
            mListener.onTabSelected(index);
        }
    }

    private void syncDroplet() {
        if (mDroplet == null || mSelected < 0 || mSelected >= mTabs.size()) {
            return;
        }
        View tab = mTabs.get(mSelected);
        if (tab.getWidth() <= 0 || mHost.getHeight() <= 0) {
            return;
        }
        int pillTop = mHost.getPaddingTop();
        int pillBottom = mHost.getHeight() - mHost.getPaddingBottom();
        int insetX = dp(6);
        int insetY = dp(4);
        int w = tab.getWidth() - insetX * 2;
        int h = Math.min(tab.getHeight(), pillBottom - pillTop) - insetY * 2;
        if (w <= 0 || h <= 0) {
            return;
        }
        if (!ViewGeom.positionIn(tab, mHost, mPos)) {
            return;
        }
        int top = mPos[1] + insetY;
        if (top + h > pillBottom) {
            top = pillBottom - h;
        }
        if (top < pillTop) {
            top = pillTop;
        }
        ViewGroup.LayoutParams lp = mDroplet.getLayoutParams();
        if (lp.width != w || lp.height != h) {
            lp.width = w;
            lp.height = h;
            mDroplet.setLayoutParams(lp);
        }
        mDroplet.setTranslationY(top - pillTop);
        mDroplet.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * mDensity);
    }
}
