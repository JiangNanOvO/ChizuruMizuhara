package com.astraflow.Chizuru.ui.glass;

import android.view.View;
import android.view.ViewGroup;

final class GlassTabStrip {

    static int tabCount(ViewGroup row) {
        return row == null ? 0 : row.getChildCount();
    }

    static View tabAt(ViewGroup row, int index) {
        if (row == null || index < 0 || index >= row.getChildCount()) return null;
        return row.getChildAt(index);
    }

    static int selectedIndex(ViewGroup row) {
        if (row == null) return 0;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child != null && child.isSelected()) return i;
        }
        return 0;
    }

    private GlassTabStrip() {
    }
}
