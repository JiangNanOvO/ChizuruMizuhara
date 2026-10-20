package com.astraflow.Chizuru.ui.glass;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.libxposed.api.XposedModule;

final class GlassParams {

    static final String PREFS = "chengcheng_cfg";

    static final String PREFS_LEGACY = "wx_liquid_glass_cfg";

    static volatile int barOffsetDp = 12;
    static volatile float panelRefractionDp = 24f;
    static volatile float panelBlurDp = 4f;
    static volatile float saturationBoost = 1.08f;
    static volatile float sampleScaleLegacy = 0.4f;
    static volatile int blurRadiusLegacy = 3;
    static volatile float dropletRefractionDp = 10f;
    static volatile float amountDp = 14f;
    static volatile float aberration = 0.5f;
    static volatile float refractionFraction = 0.18f;
    static volatile float backdropBlurDp = 4f;
    static volatile float backdropSaturation = 1.5f;
    static volatile float tabZoom = 0.1f;
    static volatile float pressScale = 78f / 56f;
    static volatile float focusScale = 1.2f;
    static volatile float stretchLimit = 0.2f;
    static volatile float settleFraction = 0.025f;
    static volatile float pillGrowthDp = 8f;
    static volatile float lastRowGapDp = 8f;
    static volatile boolean swipeSwitch = true;
    static volatile boolean hostWeChat = true;
    static volatile boolean hostQQ = true;

    private GlassParams() {
    }

    static void load(XposedModule module, Context ctx) {
        if (module != null) {
            try {
                SharedPreferences remote = module.getRemotePreferences(PREFS);
                if (remote != null) {
                    apply(remote);
                    return;
                }
            } catch (Throwable t) {
                GlassLog.err("remote config unavailable", t);
            }
        }
        try {
            apply(ctx.getSharedPreferences(PREFS_LEGACY, 0));
        } catch (Throwable t) {
            GlassLog.err("config load failed", t);
        }
    }

    private static void apply(SharedPreferences p) {
        barOffsetDp = clampInt(p.getInt("barOffsetDp", barOffsetDp), 0, 40);
        panelRefractionDp = clamp(p.getFloat("panelRefractionDp", panelRefractionDp), 0f, 60f);
        panelBlurDp = clamp(p.getFloat("panelBlurDp", panelBlurDp), 0f, 24f);
        saturationBoost = clamp(p.getFloat("saturationBoost", saturationBoost), 1f, 2.5f);
        sampleScaleLegacy = clamp(p.getFloat("sampleScaleLegacy", sampleScaleLegacy), 0.2f, 1f);
        blurRadiusLegacy = clampInt(p.getInt("blurRadiusLegacy", blurRadiusLegacy), 0, 20);
        dropletRefractionDp = clamp(p.getFloat("dropletRefractionDp", dropletRefractionDp), 0f, 40f);
        amountDp = clamp(p.getFloat("amountDp", amountDp), 0f, 40f);
        aberration = clamp(p.getFloat("aberration", aberration), 0f, 2f);
        refractionFraction = clamp(p.getFloat("refractionFraction", refractionFraction), 0f, 1f);
        backdropBlurDp = clamp(p.getFloat("backdropBlurDp", backdropBlurDp), 0f, 20f);
        backdropSaturation = clamp(p.getFloat("backdropSaturation", backdropSaturation), 1f, 3f);
        tabZoom = clamp(p.getFloat("tabZoom", tabZoom), 0f, 0.4f);
        pressScale = clamp(p.getFloat("pressScale", pressScale), 1f, 2f);
        focusScale = clamp(p.getFloat("focusScale", focusScale), 1f, 1.6f);
        stretchLimit = clamp(p.getFloat("stretchLimit", stretchLimit), 0f, 0.6f);
        settleFraction = clamp(p.getFloat("settleFraction", settleFraction), 0.005f, 0.2f);
        pillGrowthDp = clamp(p.getFloat("pillGrowthDp", pillGrowthDp), 0f, 24f);
        lastRowGapDp = clamp(p.getFloat("lastRowGapDp", lastRowGapDp), 0f, 24f);
        swipeSwitch = p.getBoolean("swipeSwitch", swipeSwitch);
        hostWeChat = p.getBoolean("hostWeChat", hostWeChat);
        hostQQ = p.getBoolean("hostQQ", hostQQ);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
