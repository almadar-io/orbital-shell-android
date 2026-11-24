package com.google.androidgamesdk;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;

/** Holds the window's first draw, and with it the platform splash, until the native surface has presented a frame. */
public final class OrbitalFirstFrame {
    private OrbitalFirstFrame() {}

    public static native boolean nativePresented();

    public static void install(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        decor.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!nativePresented()) return false;
                decor.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
    }
}
