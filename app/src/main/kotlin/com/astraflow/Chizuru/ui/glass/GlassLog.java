package com.astraflow.Chizuru.ui.glass;

import android.util.Log;

final class GlassLog {

    private static final String TAG = "Chizuru-Glass";

    static void info(String message) {
        Log.i(TAG, message);
    }

    static void err(String message, Throwable t) {
        Log.e(TAG, message, t);
    }

    private GlassLog() {
    }
}
