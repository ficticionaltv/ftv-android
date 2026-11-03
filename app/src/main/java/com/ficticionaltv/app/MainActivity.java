package com.ficticionaltv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    /* ====== CONFIGURACION: cambia solo esto si cambia tu dominio ====== */
    private static final String SITE_HOST = "ficticionaltv.vercel.app";
    private static final String PHONE_URL = "https://" + SITE_HOST + "/";
    private static final String TV_URL    = "https://" + SITE_HOST + "/tv/";
    /* ================================================================== */

    private static final String OFFLINE_URL = "file:///android_asset/offline.html";

    private FrameLayout root;
    private WebView web;
    private boolean isTv;
    private String targetUrl;
    private boolean failed = false;

    // Pantalla completa de video (botón fullscreen de YouTube, etc.)
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private WebChromeClient chrome;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isTv = detectTv();
        targetUrl = isTv ? TV_URL : PHONE_URL;

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0E0E12);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0E0E12);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                    // "continuar viendo", sesión, etc.
        s.setMediaPlaybackRequiresUserGesture(false);    // autoplay de los reproductores
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setUserAgentString(s.getUserAgentString() + " FTVApp/1.0" + (isTv ? " FTVTV" : " FTVPhone"));

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);        // los iframes de video lo necesitan

        // Puente que tu tv.js ya espera: window.AndroidTV.exit()
        web.addJavascriptInterface(new Bridge(), "AndroidTV");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                if (!req.isForMainFrame()) return false;
                Uri u = req.getUrl();
                String host = u.getHost();
                if ("file".equals(u.getScheme())) return false;
                if (host != null && host.equals(SITE_HOST)) return false;
                if (!isTv) {   // en el teléfono, enlaces externos van al navegador
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                }
                return true;   // en TV se bloquean (no hay navegador)
            }
            @Override
            public void onPageStarted(WebView v, String url, Bitmap favicon) {
                if (url != null && !url.startsWith("file:")) failed = false;
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame() && !failed) {
                    failed = true;
                    v.loadUrl(OFFLINE_URL);
                }
            }
        });

        chrome = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = view; customCallback = cb;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
                enterImmersive();
            }
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                root.removeView(customView);
                customView = null;
                if (customCallback != null) customCallback.onCustomViewHidden();
                customCallback = null;
                web.setVisibility(View.VISIBLE);
                if (isTv) enterImmersive();
            }
        };
        web.setWebChromeClient(chrome);

        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();

        if (isTv) enterImmersive();
        web.loadUrl(targetUrl);
    }

    private boolean detectTv() {
        UiModeManager ui = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (ui != null && ui.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) return true;
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @SuppressWarnings("deprecation")
    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && (isTv || customView != null)) enterImmersive();
    }

    /* Botón Atrás */
    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (customView != null) {                       // salir de video a pantalla completa
            chrome.onHideCustomView();
            return;
        }
        if (failed) { finish(); return; }               // en la pantalla sin conexión: salir
        if (isTv) {
            // tu tv.js decide qué hacer (cerrar modal, volver, ir al menú o salir vía AndroidTV.exit())
            web.evaluateJavascript(
                "(function(){var e=new KeyboardEvent('keydown',{key:'GoBack',bubbles:true,cancelable:true});"
              + "try{Object.defineProperty(e,'keyCode',{get:function(){return 4}});}catch(x){}"
              + "(document.activeElement||document).dispatchEvent(e);})();", null);
        } else if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() { super.onPause(); web.onPause(); }
    @Override
    protected void onResume() { super.onResume(); web.onResume(); }
    @Override
    protected void onDestroy() {
        if (web != null) { root.removeView(web); web.destroy(); }
        super.onDestroy();
    }

    /* Funciones que la web puede llamar desde JavaScript */
    private class Bridge {
        @JavascriptInterface
        public void exit() { runOnUiThread(new Runnable() { public void run() { finish(); } }); }

        @JavascriptInterface
        public void retry() {
            runOnUiThread(new Runnable() { public void run() { failed = false; web.loadUrl(targetUrl); } });
        }
    }
}
