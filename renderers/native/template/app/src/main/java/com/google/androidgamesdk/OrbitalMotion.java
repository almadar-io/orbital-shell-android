package com.google.androidgamesdk;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

public final class OrbitalMotion {
    private static ContentObserver observer;

    private OrbitalMotion() {}

    public static native void nativeSetReducedMotion(boolean reduced);

    public static void refresh(Context context) {
        float scale = Settings.Global.getFloat(
            context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        nativeSetReducedMotion(scale == 0f);
    }

    public static void install(Context context) {
        refresh(context);
        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                refresh(context);
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer);
    }

    public static void clear(Context context) {
        if (observer != null) {
            context.getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
    }
}
