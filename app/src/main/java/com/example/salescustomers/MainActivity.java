package com.example.salescustomers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // تشغيل JavaScript
        settings.setJavaScriptEnabled(true);

        // تشغيل التخزين المحلي
        settings.setDomStorageEnabled(true);

        // دعم قواعد البيانات المحلية
        settings.setDatabaseEnabled(true);

        // السماح بملفات التطبيق المحلية
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // تحسين العرض على الهاتف
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);

        // منع التكبير غير الضروري
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // إبقاء الروابط داخل WebView
        webView.setWebViewClient(
            new WebViewClient()
        );

        // جسر JavaScript مع Android
        webView.addJavascriptInterface(
            new AndroidFirebaseBridge(),
            "AndroidFirebase"
        );

        // تحميل التطبيق
        webView.loadUrl(
            "file:///android_asset/index.html"
        );
    }

    public class AndroidFirebaseBridge {

        @JavascriptInterface
        public String getUserUid() {
            try {
                return com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .getCurrentUser() != null
                    ? com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid()
                    : "";
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getUserEmail() {
            try {
                if (
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser() != null
                ) {
                    String email =
                        com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getEmail();

                    return email != null ? email : "";
                }
            } catch (Exception ignored) {
            }

            return "";
        }

        @JavascriptInterface
        public String getUserName() {
            try {
                if (
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser() != null
                ) {
                    String name =
                        com.google.firebase.auth.FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getDisplayName();

                    return name != null ? name : "";
                }
            } catch (Exception ignored) {
            }

            return "";
        }

        @JavascriptInterface
        public boolean isLoggedIn() {
            try {
                return com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .getCurrentUser() != null;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void logout() {
            runOnUiThread(() -> {

                try {
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .signOut();
                } catch (Exception ignored) {
                }

                finish();

            });
        }

        @JavascriptInterface
        public void showMessage(String message) {

            runOnUiThread(() -> {

                android.widget.Toast.makeText(
                    MainActivity.this,
                    message == null ? "" : message,
                    android.widget.Toast.LENGTH_SHORT
                ).show();

            });
        }
    }

    @Override
    public void onBackPressed() {

        if (
            webView != null &&
            webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();

        }
    }
}
