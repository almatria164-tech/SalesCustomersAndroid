package com.example.salescustomers;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String localData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        /*
         * ربط Java مع JavaScript الموجود داخل index.html
         *
         * في index.html عندك:
         * AndroidFirebase.saveData(...)
         * AndroidFirebase.loadData()
         * AndroidFirebase.getUserUid()
         */
        webView.addJavascriptInterface(
                new AndroidFirebaseBridge(),
                "AndroidFirebase"
        );

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("file:///android_asset/index.html");
    }


    // =========================================================
    // AndroidFirebase Bridge
    // =========================================================

    public class AndroidFirebaseBridge {

        /*
         * حفظ البيانات
         *
         * index.html يستدعي:
         *
         * AndroidFirebase.saveData(JSON.stringify(db));
         */
        @JavascriptInterface
        public void saveData(String jsonData) {

            if (jsonData == null) {
                return;
            }

            localData = jsonData;

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                return;
            }

            String uid = user.getUid();

            Map<String, Object> data = new HashMap<>();
            data.put("json", jsonData);

            firestore
                    .collection("users")
                    .document(uid)
                    .collection("appData")
                    .document("sales")
                    .set(data)
                    .addOnSuccessListener(unused -> {

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "تم حفظ البيانات",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });

                    })
                    .addOnFailureListener(e -> {

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "تعذر حفظ البيانات",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });

                    });
        }


        /*
         * تحميل البيانات
         *
         * هذا يرجع آخر نسخة محلية فورًا،
         * ثم نحاول تحميل النسخة الموجودة في Firebase.
         */
        @JavascriptInterface
        public String loadData() {

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                return localData;
            }

            String uid = user.getUid();

            firestore
                    .collection("users")
                    .document(uid)
                    .collection("appData")
                    .document("sales")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (documentSnapshot.exists()) {

                            String json =
                                    documentSnapshot.getString("json");

                            if (json != null && !json.isEmpty()) {

                                localData = json;

                                /*
                                 * إرسال البيانات الجديدة إلى index.html
                                 */
                                final String safeJson =
                                        escapeForJavaScript(json);

                                runOnUiThread(() -> {

                                    webView.evaluateJavascript(
                                            "if(typeof onCloudDataLoaded === 'function')" +
                                            "{onCloudDataLoaded(" +
                                            safeJson +
                                            ");}",
                                            null
                                    );

                                });
                            }
                        }

                    })
                    .addOnFailureListener(e -> {
                        // نترك البيانات المحلية كما هي
                    });

            return localData;
        }


        /*
         * الحصول على UID الخاص بالمستخدم الحالي
         *
         * index.html يستدعي:
         *
         * AndroidFirebase.getUserUid()
         */
        @JavascriptInterface
        public String getUserUid() {

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null) {
                return user.getUid();
            }

            return "";
        }


        /*
         * الحصول على البريد الإلكتروني للمستخدم الحالي
         */
        @JavascriptInterface
        public String getUserEmail() {

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null && user.getEmail() != null) {
                return user.getEmail();
            }

            return "";
        }


        /*
         * الحصول على اسم المستخدم
         */
        @JavascriptInterface
        public String getUserName() {

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null && user.getDisplayName() != null) {
                return user.getDisplayName();
            }

            return "";
        }


        /*
         * تسجيل الخروج
         */
        @JavascriptInterface
        public void logout() {

            firebaseAuth.signOut();

            runOnUiThread(() -> {

                Toast.makeText(
                        MainActivity.this,
                        "تم تسجيل الخروج",
                        Toast.LENGTH_SHORT
                ).show();

                webView.reload();
            });
        }


        /*
         * فحص هل يوجد مستخدم مسجل دخول
         */
        @JavascriptInterface
        public boolean isLoggedIn() {

            return firebaseAuth.getCurrentUser() != null;
        }


        /*
         * عرض رسالة من JavaScript
         */
        @JavascriptInterface
        public void showMessage(String message) {

            runOnUiThread(() -> {

                Toast.makeText(
                        MainActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();

            });
        }
    }


    // =========================================================
    // حماية النص قبل إرساله إلى JavaScript
    // =========================================================

    private String escapeForJavaScript(String value) {

        if (value == null) {
            return "null";
        }

        String escaped = value;

        escaped = escaped
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\'", "\\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("</", "<\\/");

        return "\"" + escaped + "\"";
    }


    // =========================================================
    // زر الرجوع
    // =========================================================

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    // =========================================================
    // تنظيف WebView
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
