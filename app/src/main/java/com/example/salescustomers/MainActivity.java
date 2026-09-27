package com.example.salescustomers;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

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

        webView.addJavascriptInterface(
                new AndroidFirebaseBridge(),
                "AndroidFirebase"
        );

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class AndroidFirebaseBridge {

        @JavascriptInterface
        public void saveData(String jsonData) {

            if (jsonData == null || jsonData.isEmpty()) {
                showToast("لا توجد بيانات للحفظ");
                return;
            }

            localData = jsonData;

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                showToast("خطأ: المستخدم غير مسجل الدخول");
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
                                    "تم حفظ البيانات بنجاح",
                                    Toast.LENGTH_SHORT
                            ).show();

                        });

                    })
                    .addOnFailureListener(e -> {

                        String errorCode = "UNKNOWN";

                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreException =
                                    (FirebaseFirestoreException) e;

                            errorCode =
                                    firestoreException
                                            .getCode()
                                            .name();
                        }

                        String errorMessage = e.getMessage();

                        if (errorMessage == null ||
                                errorMessage.isEmpty()) {
                            errorMessage = "سبب غير معروف";
                        }

                        final String finalMessage =
                                "خطأ الحفظ:\n"
                                        + errorCode
                                        + "\n"
                                        + errorMessage;

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    MainActivity.this,
                                    finalMessage,
                                    Toast.LENGTH_LONG
                            ).show();

                        });

                    });
        }

        @JavascriptInterface
        public String loadData() {

            FirebaseUser user =
                    firebaseAuth.getCurrentUser();

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

                            if (json != null &&
                                    !json.isEmpty()) {

                                localData = json;

                                final String safeJson =
                                        escapeForJavaScript(json);

                                runOnUiThread(() -> {

                                    webView.evaluateJavascript(
                                            "if(typeof loadCloudData === 'function')" +
                                            "{loadCloudData(" +
                                            safeJson +
                                            ");}",
                                            null
                                    );

                                });
                            }
                        }

                    })
                    .addOnFailureListener(e -> {

                        String errorCode = "UNKNOWN";

                        if (e instanceof FirebaseFirestoreException) {

                            FirebaseFirestoreException firestoreException =
                                    (FirebaseFirestoreException) e;

                            errorCode =
                                    firestoreException
                                            .getCode()
                                            .name();
                        }

                        String errorMessage = e.getMessage();

                        if (errorMessage == null ||
                                errorMessage.isEmpty()) {
                            errorMessage = "سبب غير معروف";
                        }

                        final String finalMessage =
                                "خطأ تحميل البيانات:\n"
                                        + errorCode
                                        + "\n"
                                        + errorMessage;

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    MainActivity.this,
                                    finalMessage,
                                    Toast.LENGTH_LONG
                            ).show();

                        });

                    });

            return localData;
        }

        @JavascriptInterface
        public String getUserUid() {

            FirebaseUser user =
                    firebaseAuth.getCurrentUser();

            if (user != null) {
                return user.getUid();
            }

            return "";
        }

        @JavascriptInterface
        public String getUserEmail() {

            FirebaseUser user =
                    firebaseAuth.getCurrentUser();

            if (user != null &&
                    user.getEmail() != null) {

                return user.getEmail();
            }

            return "";
        }

        @JavascriptInterface
        public String getUserName() {

            FirebaseUser user =
                    firebaseAuth.getCurrentUser();

            if (user != null &&
                    user.getDisplayName() != null) {

                return user.getDisplayName();
            }

            return "";
        }

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

        @JavascriptInterface
        public boolean isLoggedIn() {

            return firebaseAuth.getCurrentUser() != null;
        }

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

    private void showToast(String message) {

        runOnUiThread(() -> {

            Toast.makeText(
                    MainActivity.this,
                    message,
                    Toast.LENGTH_LONG
            ).show();

        });
    }

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

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();

        }
    }

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
