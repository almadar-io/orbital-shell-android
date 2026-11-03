package com.google.androidgamesdk;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.util.HashMap;
import java.util.Map;

public final class OrbitalVideoInput {
    private static Activity activity;
    private static View surface;
    private static final Map<Integer, WebView> views = new HashMap<>();
    private static final Map<Integer, String> sources = new HashMap<>();

    private OrbitalVideoInput() {}

    public static void install(Activity host, View gpuSurface) {
        activity = host;
        surface = gpuSurface;
        nativeInstall();
    }

    public static void upsert(int nodeId, String videoId, String url, String title, String html,
                              boolean autoplay, boolean controls, double start,
                              float x, float y, float width, float height) {
        Activity host = activity;
        if (host == null) return;
        host.runOnUiThread(() -> {
            if (activity != host || width <= 0 || height <= 0) {
                removeOnUiThread(nodeId);
                return;
            }
            boolean youtube = videoId != null && !videoId.isEmpty();
            String source = youtube ? videoId : url;
            if (source == null || source.isEmpty()) {
                removeOnUiThread(nodeId);
                return;
            }
            WebView web = views.get(nodeId);
            String identity = (youtube ? "yt:" : "url:") + source + ":" + autoplay + ":" + controls + ":" + start;
            if (web == null) {
                web = new WebView(host);
                web.setBackgroundColor(Color.BLACK);
                web.getSettings().setJavaScriptEnabled(true);
                web.getSettings().setDomStorageEnabled(true);
                web.getSettings().setAllowFileAccess(true);
                web.getSettings().setMediaPlaybackRequiresUserGesture(false);
                web.setWebChromeClient(new WebChromeClient());
                views.put(nodeId, web);
                ((ViewGroup) host.getWindow().getDecorView()).addView(web);
            }
            web.setContentDescription(title);
            int[] surfaceOrigin = new int[2];
            surface.getLocationInWindow(surfaceOrigin);
            FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
            layout.leftMargin = surfaceOrigin[0] + Math.round(x);
            layout.topMargin = surfaceOrigin[1] + Math.round(y);
            web.setLayoutParams(layout);
            if (identity.equals(sources.get(nodeId))) return;
            sources.put(nodeId, identity);
            if (youtube) {
                web.setWebViewClient(new WebViewClient());
                web.addJavascriptInterface(new YouTubeBridge(nodeId, source), "OrbitalVideo");
                web.loadDataWithBaseURL("https://www.youtube.com", html,
                    "text/html", "UTF-8", null);
            } else {
                web.removeJavascriptInterface("OrbitalVideo");
                web.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String loadedUrl) {
                        nativeVideoEvent(nodeId, 0, source, "", "");
                    }
                });
                web.loadUrl(source);
            }
        });
    }

    public static void remove(int nodeId) {
        Activity host = activity;
        if (host != null) host.runOnUiThread(() -> removeOnUiThread(nodeId));
    }

    public static void clear() {
        Activity host = activity;
        if (host == null) return;
        host.runOnUiThread(() -> {
            for (Integer id : views.keySet().toArray(new Integer[0])) removeOnUiThread(id);
            activity = null;
            surface = null;
        });
    }

    private static void removeOnUiThread(int nodeId) {
        WebView web = views.remove(nodeId);
        sources.remove(nodeId);
        if (web == null) return;
        ((ViewGroup) web.getParent()).removeView(web);
        web.destroy();
    }

    private static final class YouTubeBridge {
        private final int nodeId;
        private final String source;

        YouTubeBridge(int id, String name) {
            nodeId = id;
            source = name;
        }

        @JavascriptInterface public void ready() { nativeVideoEvent(nodeId, 0, source, "", ""); }
        @JavascriptInterface public void play() { nativeVideoEvent(nodeId, 1, source, "", ""); }
        @JavascriptInterface public void error(String code) {
            nativeVideoEvent(nodeId, 2, source, "Video playback failed", code);
        }
    }

    private static native void nativeVideoEvent(int nodeId, int kind, String source, String error, String code);
    private static native void nativeInstall();
}
