package com.example.salescustomers;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // WebView
        webView = new WebView(this);

        // Root container
        FrameLayout root = new FrameLayout(this);

        FrameLayout.LayoutParams webViewParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        root.addView(webView, webViewParams);

        // Show root instead of WebView directly
        setContentView(root);

        /*
         * Keep the app content away from:
         * - Status bar
         * - Navigation bar
         * - Android gesture areas
         */
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets systemBars =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    Insets systemGestures =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemGestures()
                            );

                    int left = Math.max(
                            systemBars.left,
                            systemGestures.left
                    );

                    int top = Math.max(
                            systemBars.top,
                            systemGestures.top
                    );

                    int right = Math.max(
                            systemBars.right,
                            systemGestures.right
                    );

                    int bottom = Math.max(
                            systemBars.bottom,
                            systemGestures.bottom
                    );

                    view.setPadding(
                            left,
                            top,
                            right,
                            bottom
                    );

                    return windowInsets;
                }
        );

        // WebView settings
        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Local Storage
        settings.setDomStorageEnabled(true);

        // Database
        settings.setDatabaseEnabled(true);

        // File access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Responsive WebView
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);

        // Disable unnecessary zoom controls
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Keep links inside WebView
        webView.setWebViewClient(new WebViewClient());

        // Android ↔ JavaScript bridge
        webView.addJavascriptInterface(
                new AndroidBridge(),
                "Android"
        );

        // Compatibility with older JS bridge name
        webView.addJavascriptInterface(
                new AndroidBridge(),
                "AndroidFirebase"
        );

        // Load app
        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
