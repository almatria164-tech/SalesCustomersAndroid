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

import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
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

        webView.setWebViewClient(new WebViewClient());

        // جسر بين JavaScript و Firebase
        webView.addJavascriptInterface(new FirebaseBridge(), "AndroidFirebase");

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class FirebaseBridge {

        @JavascriptInterface
        public String getUserUid() {
            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                return "";
            }

            return user.getUid();
        }

        @JavascriptInterface
        public String getUserEmail() {
            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                return "";
            }

            return user.getEmail() == null ? "" : user.getEmail();
        }

        @JavascriptInterface
        public void saveData(String jsonData) {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "يجب تسجيل الدخول أولاً",
                                Toast.LENGTH_SHORT
                        ).show()
                );
                return;
            }

            try {
                JSONObject jsonObject = new JSONObject(jsonData);

                firestore
                        .collection("users")
                        .document(user.getUid())
                        .collection("app")
                        .document("data")
                        .set(
                                new java.util.HashMap<String, Object>() {{
                                    put("data", jsonObject.toString());
                                }}
                        )
                        .addOnFailureListener(e ->
                                runOnUiThread(() ->
                                        Toast.makeText(
                                                MainActivity.this,
                                                "تعذر حفظ البيانات",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                )
                        );

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "خطأ في البيانات",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }
        }

        @JavascriptInterface
        public void loadData() {

            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                return;
            }

            firestore
                    .collection("users")
                    .document(user.getUid())
                    .collection("app")
                    .document("data")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (!documentSnapshot.exists()) {
                            return;
                        }

                        String data = documentSnapshot.getString("data");

                        if (data == null) {
                            return;
                        }

                        String escaped = JSONObject.quote(data);

                        runOnUiThread(() -> {

                            webView.evaluateJavascript(
                                    "window.loadCloudData(" + escaped + ");",
                                    null
                            );

                        });
                    });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
