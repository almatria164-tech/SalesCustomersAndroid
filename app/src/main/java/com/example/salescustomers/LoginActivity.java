package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
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

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.security.SecureRandom;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;

    private Button loginButton;
    private Button registerButton;
    private Button googleButton;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private String webClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        webClientId = getString(R.string.default_web_client_id);

        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleButton = findViewById(R.id.googleButton);

        loginButton.setOnClickListener(v -> loginWithEmail());

        registerButton.setOnClickListener(v -> registerWithEmail());

        googleButton.setOnClickListener(v -> loginWithGoogle());
    }

    // =========================
    // Email Login
    // =========================

    private void loginWithEmail() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "أدخل البريد الإلكتروني وكلمة المرور",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "تعذر تسجيل الدخول";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================
    // Register
    // =========================

    private void registerWithEmail() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "أدخل البريد الإلكتروني وكلمة المرور",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(
                    this,
                    "كلمة المرور يجب أن تكون 6 أحرف على الأقل",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    registerButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "تعذر إنشاء الحساب";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================
    // Google Login
    // =========================

    private void loginWithGoogle() {

        googleButton.setEnabled(false);

        try {

            GetSignInWithGoogleOption googleOption =
                    new GetSignInWithGoogleOption.Builder(webClientId)
                            .setNonce(generateNonce())
                            .build();

            GetCredentialRequest request =
                    new GetCredentialRequest.Builder()
                            .addCredentialOption(googleOption)
                            .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    Runnable::run,
                    new androidx.credentials.CredentialManagerCallback<
                            GetCredentialResponse,
                            GetCredentialException>() {

                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleGoogleCredential(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {

                            runOnUiThread(() -> {

                                googleButton.setEnabled(true);

                                Toast.makeText(
                                        LoginActivity.this,
                                        "تعذر تسجيل الدخول باستخدام Google\n"
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );

        } catch (Exception e) {

            googleButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "حدث خطأ أثناء تشغيل تسجيل الدخول بواسطة Google",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================
    // Handle Google Credential
    // =========================

    private void handleGoogleCredential(GetCredentialResponse result) {

        Credential credential = result.getCredential();

        if (!(credential instanceof CustomCredential)) {

            runOnUiThread(() -> {

                googleButton.setEnabled(true);

                Toast.makeText(
                        LoginActivity.this,
                        "بيانات Google غير صالحة",
                        Toast.LENGTH_LONG
                ).show();
            });

            return;
        }

        CustomCredential customCredential =
                (CustomCredential) credential;

        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(customCredential.getType())) {

            runOnUiThread(() -> {

                googleButton.setEnabled(true);

                Toast.makeText(
                        LoginActivity.this,
                        "نوع بيانات Google غير مدعوم",
                        Toast.LENGTH_LONG
                ).show();
            });

            return;
        }

        try {

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(
                            customCredential.getData()
                    );

            String idToken = googleCredential.getIdToken();

            firebaseLoginWithGoogle(idToken);

                } catch (Exception e) {
            runOnUiThread(() -> {
                googleButton.setEnabled(true);
                Toast.makeText(
                        LoginActivity.this,
                        "تعذر قراءة بيانات Google",
                        Toast.LENGTH_LONG
                ).show();
            });
        }
    }

    // =========================
    // Firebase Google Login
    // =========================

    private void firebaseLoginWithGoogle(String idToken) {

        AuthCredential firebaseCredential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        mAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this, task -> {

                    googleButton.setEnabled(true);

                    if (task.isSuccessful()) {

                        openMainActivity();

                    } else {

                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "تعذر تسجيل الدخول باستخدام Google";

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================
    // Secure Nonce
    // =========================

    private String generateNonce() {

        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.encodeToString(
                bytes,
                Base64.NO_WRAP
                        | Base64.NO_PADDING
                        | Base64.URL_SAFE
        );
    }

    // =========================
    // Open Main App
    // =========================

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        startActivity(intent);

        finish();
    }
}
