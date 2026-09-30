package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private EditText emailEditText;
    private EditText passwordEditText;

    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        executor = getMainExecutor();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);
        Button googleButton = findViewById(R.id.googleButton);

        loginButton.setOnClickListener(v -> loginWithEmail());

        registerButton.setOnClickListener(v -> registerWithEmail());

        googleButton.setOnClickListener(v -> loginWithGoogle());

        // إذا كان المستخدم مسجلاً مسبقاً
        if (mAuth.getCurrentUser() != null) {
            openMainActivity();
        }
    }

    // =========================
    // تسجيل الدخول بالبريد
    // =========================

    private void loginWithEmail() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            showMessage("أدخل البريد الإلكتروني");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showMessage("أدخل كلمة المرور");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        String message = "تعذر تسجيل الدخول";

                        if (task.getException() != null &&
                                task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        showMessage(message);
                    }
                });
    }

    // =========================
    // إنشاء حساب بالبريد
    // =========================

    private void registerWithEmail() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            showMessage("أدخل البريد الإلكتروني");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showMessage("أدخل كلمة المرور");
            return;
        }

        if (password.length() < 6) {
            showMessage("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        String message = "تعذر إنشاء الحساب";

                        if (task.getException() != null &&
                                task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        showMessage(message);
                    }
                });
    }

    // =========================
    // تسجيل الدخول باستخدام Google
    // =========================

    private void loginWithGoogle() {

        String webClientId;

        try {
            webClientId = getString(R.string.default_web_client_id);
        } catch (Exception e) {
            showMessage("لم يتم العثور على Web Client ID الخاص بـ Google");
            return;
        }

        if (TextUtils.isEmpty(webClientId)) {
            showMessage("Web Client ID غير موجود");
            return;
        }

        // أول محاولة:
        // الحسابات التي سبق أن استخدمت التطبيق
        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            GetCredentialResponse result) {

                        handleGoogleCredential(result);
                    }

                    @Override
                    public void onError(
                            GetCredentialException e) {

                        // أهم إصلاح:
                        // إذا لم يوجد حساب مصرح به،
                        // نعيد الطلب مع السماح بكل حسابات Google.
                        if (e instanceof NoCredentialException) {
                            loginWithGoogleAllAccounts(webClientId);
                        } else {
                            showMessage(
                                    "تعذر تسجيل الدخول باستخدام Google\n"
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    // =========================
    // محاولة Google بكل الحسابات
    // =========================

    private void loginWithGoogleAllAccounts(String webClientId) {

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            GetCredentialResponse result) {

                        handleGoogleCredential(result);
                    }

                    @Override
                    public void onError(
                            GetCredentialException e) {

                        showMessage(
                                "تعذر تسجيل الدخول باستخدام Google\n"
                                        + e.getMessage()
                        );
                    }
                }
        );
    }

    // =========================
    // معالجة حساب Google
    // =========================

    private void handleGoogleCredential(
            GetCredentialResponse result) {

        Credential credential = result.getCredential();

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {

                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData()
                            );

                    String idToken =
                            googleCredential.getIdToken();

                    firebaseAuthWithGoogle(idToken);

                } catch (GoogleIdTokenParsingException e) {

                    showMessage(
                            "تعذر قراءة بيانات حساب Google"
                    );
                }

            } else {

                showMessage(
                        "نوع بيانات Google غير معروف"
                );
            }

        } else {

            showMessage(
                    "لم يتم استلام بيانات حساب Google"
            );
        }
    }

    // =========================
    // Firebase + Google
    // =========================

    private void firebaseAuthWithGoogle(
            String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                openMainActivity();

                            } else {

                                String message =
                                        "تعذر تسجيل الدخول إلى Firebase";

                                if (task.getException() != null &&
                                        task.getException().getMessage() != null) {

                                    message =
                                            task.getException().getMessage();
                                }

                                showMessage(message);
                            }
                        }
                );
    }

    // =========================
    // فتح التطبيق
    // =========================

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================
    // رسالة
    // =========================

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
