package com.example.salescustomers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CredentialManager credentialManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        credentialManager =
                CredentialManager.create(this);

        webView = new WebView(this);

        setContentView(webView);

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

        // Keep compatibility with older JS bridge name
        webView.addJavascriptInterface(
                new AndroidBridge(),
                "AndroidFirebase"
        );

        // Load app
        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    // =========================================================
    // ANDROID BRIDGE
    // =========================================================

    public class AndroidBridge {

        // -----------------------------------------------------
        // USER INFORMATION
        // -----------------------------------------------------

        @JavascriptInterface
        public String getUserUid() {

            try {

                if (firebaseAuth.getCurrentUser() != null) {

                    return firebaseAuth
                            .getCurrentUser()
                            .getUid();
                }

            } catch (Exception ignored) {
            }

            return "";
        }

        @JavascriptInterface
        public String getUserEmail() {

            try {

                if (firebaseAuth.getCurrentUser() != null) {

                    String email =
                            firebaseAuth
                                    .getCurrentUser()
                                    .getEmail();

                    return email != null
                            ? email
                            : "";
                }

            } catch (Exception ignored) {
            }

            return "";
        }

        @JavascriptInterface
        public String getUserName() {

            try {

                if (firebaseAuth.getCurrentUser() != null) {

                    String name =
                            firebaseAuth
                                    .getCurrentUser()
                                    .getDisplayName();

                    return name != null
                            ? name
                            : "";
                }

            } catch (Exception ignored) {
            }

            return "";
        }

        @JavascriptInterface
        public boolean isLoggedIn() {

            try {

                return firebaseAuth.getCurrentUser() != null;

            } catch (Exception e) {

                return false;
            }
        }

        // -----------------------------------------------------
        // SAVE CLOUD DATA
        // -----------------------------------------------------

        @JavascriptInterface
        public void saveCloudData(String json) {

            runOnUiThread(() -> {

                if (firebaseAuth.getCurrentUser() == null) {

                    sendToast(
                            "يجب تسجيل الدخول أولاً"
                    );

                    return;
                }

                String uid =
                        firebaseAuth
                                .getCurrentUser()
                                .getUid();

                Map<String, Object> data =
                        new HashMap<>();

                data.put(
                        "json",
                        json == null ? "{}" : json
                );

                data.put(
                        "updatedAt",
                        com.google.firebase.firestore.FieldValue
                                .serverTimestamp()
                );

                firestore
                        .collection("users")
                        .document(uid)
                        .collection("appData")
                        .document("sales")
                        .set(
                                data,
                                SetOptions.merge()
                        )
                        .addOnSuccessListener(
                                unused -> {
                                    // تم الحفظ
                                }
                        )
                        .addOnFailureListener(
                                error -> {

                                    sendToast(
                                            "تعذر حفظ البيانات السحابية"
                                    );

                                }
                        );

            });
        }

        // -----------------------------------------------------
        // LOAD CLOUD DATA
        // -----------------------------------------------------

        @JavascriptInterface
        public void loadCloudData() {

            runOnUiThread(() -> {

                if (firebaseAuth.getCurrentUser() == null) {

                    sendJavascript(
                            "window.onCloudDataLoaded('{}');"
                    );

                    return;
                }

                String uid =
                        firebaseAuth
                                .getCurrentUser()
                                .getUid();

                firestore
                        .collection("users")
                        .document(uid)
                        .collection("appData")
                        .document("sales")
                        .get()
                        .addOnSuccessListener(
                                document -> {

                                    String json = "{}";

                                    if (
                                            document.exists()
                                                    &&
                                            document.contains("json")
                                    ) {

                                        String saved =
                                                document.getString(
                                                        "json"
                                                );

                                        if (
                                                saved != null
                                                        &&
                                                !saved.trim().isEmpty()
                                        ) {

                                            json = saved;
                                        }
                                    }

                                    sendCloudDataToJavascript(
                                            json
                                    );

                                }
                        )
                        .addOnFailureListener(
                                error -> {

                                    sendCloudDataToJavascript(
                                            "{}"
                                    );

                                }
                        );

            });
        }

        // -----------------------------------------------------
        // LOGOUT
        // -----------------------------------------------------

        @JavascriptInterface
        public void logout() {

            runOnUiThread(() -> {

                // First sign out from Firebase
                try {

                    firebaseAuth.signOut();

                } catch (Exception ignored) {
                }

                // Then clear Credential Manager state
                try {

                    ClearCredentialStateRequest request =
                            new ClearCredentialStateRequest();

                    credentialManager.clearCredentialStateAsync(
                            request,
                            null,
                            Runnable::run,
                            new CredentialManagerCallback<
                                    Void,
                                    ClearCredentialException>() {

                                @Override
                                public void onResult(
                                        Void result) {

                                    openLoginScreen();
                                }

                                @Override
                                public void onError(
                                        ClearCredentialException e) {

                                    // Firebase logout already happened.
                                    // Continue to login screen.
                                    openLoginScreen();
                                }
                            }
                    );

                } catch (Exception ignored) {

                    openLoginScreen();
                }

            });
        }

        // -----------------------------------------------------
        // MESSAGE
        // -----------------------------------------------------

        @JavascriptInterface
        public void showMessage(String message) {

            runOnUiThread(() -> {

                sendToast(
                        message == null
                                ? ""
                                : message
                );

            });
        }

        // -----------------------------------------------------
        // PRINT
        // -----------------------------------------------------

        @JavascriptInterface
        public void printPage(String html) {

            runOnUiThread(() -> {

                if (
                        html == null
                                ||
                        html.trim().isEmpty()
                ) {

                    sendToast(
                            "لا توجد بيانات للطباعة"
                    );

                    return;
                }

                WebView printWebView =
                        new WebView(
                                MainActivity.this
                        );

                WebSettings printSettings =
                        printWebView.getSettings();

                printSettings.setJavaScriptEnabled(false);

                printWebView.setWebViewClient(
                        new WebViewClient() {

                            @Override
                            public void onPageFinished(
                                    WebView view,
                                    String url
                            ) {

                                PrintManager printManager =
                                        (PrintManager)
                                                getSystemService(
                                                        PRINT_SERVICE
                                                );

                                if (printManager == null) {

                                    sendToast(
                                            "خدمة الطباعة غير متاحة"
                                    );

                                    return;
                                }

                                String jobName =
                                        "Smart Nota";

                                android.print.PrintDocumentAdapter
                                        printAdapter =
                                        view.createPrintDocumentAdapter(
                                                jobName
                                        );

                                PrintAttributes attributes =
                                        new PrintAttributes.Builder()
                                                .setMediaSize(
                                                        PrintAttributes.MediaSize.ISO_A4
                                                )
                                                .setMinMargins(
                                                        PrintAttributes.Margins.NO_MARGINS
                                                )
                                                .build();

                                printManager.print(
                                        jobName,
                                        printAdapter,
                                        attributes
                                );
                            }
                        }
                );

                printWebView.loadDataWithBaseURL(
                        null,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                );

            });
        }
    }

    // =========================================================
    // OPEN LOGIN SCREEN
    // =========================================================

    private void openLoginScreen() {

        runOnUiThread(() -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            LoginActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                            |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            startActivity(intent);

            finish();

        });
    }

    // =========================================================
    // JAVASCRIPT CALLBACKS
    // =========================================================

    private void sendCloudDataToJavascript(
            String json
    ) {

        if (json == null) {
            json = "{}";
        }

        final String safeJson =
                json
                        .replace(
                                "\\",
                                "\\\\"
                        )
                        .replace(
                                "'",
                                "\\'"
                        )
                        .replace(
                                "\r",
                                "\\r"
                        )
                        .replace(
                                "\n",
                                "\\n"
                        );

        sendJavascript(
                "window.onCloudDataLoaded('" +
                        safeJson +
                        "');"
        );
    }

    private void sendJavascript(
            String javascript
    ) {

        runOnUiThread(() -> {

            if (webView == null) {
                return;
            }

            webView.evaluateJavascript(
                    javascript,
                    null
            );

        });
    }

    private void sendToast(
            String message
    ) {

        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (
                webView != null
                        &&
                webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.loadUrl(
                    "about:blank"
            );

            webView.clearHistory();

            webView.removeAllViews();

            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }
}
