package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private EditText emailEditText;
    private EditText passwordEditText;

    private Button loginButton;
    private Button registerButton;
    private Button googleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleButton = findViewById(R.id.googleButton);

        loginButton.setOnClickListener(v -> loginWithEmail());
        registerButton.setOnClickListener(v -> registerWithEmail());
        googleButton.setOnClickListener(v -> loginWithGoogle());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            openMainActivity();
        }
    }

    private void loginWithEmail() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty()) {
            emailEditText.setError("أدخل البريد الإلكتروني");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("أدخل كلمة المرور");
            return;
        }

        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        showError(task.getException(), "تعذر تسجيل الدخول");
                    }
                });
    }

    private void registerWithEmail() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty()) {
            emailEditText.setError("أدخل البريد الإلكتروني");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("أدخل كلمة المرور");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError(
                    "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
            );
            return;
        }

        registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    registerButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        openMainActivity();
                    } else {
                        showError(
                                task.getException(),
                                "تعذر إنشاء الحساب"
                        );
                    }
                });
    }

    private void loginWithGoogle() {

        googleButton.setEnabled(false);

        try {

            String webClientId =
                    getString(R.string.default_web_client_id);

            GetSignInWithGoogleOption googleOption =
                    new GetSignInWithGoogleOption.Builder(
                            webClientId
                    ).build();

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
                        public void onResult(
                                GetCredentialResponse result) {

                            runOnUiThread(() ->
                                    handleGoogleCredential(result)
                            );
                        }

                        @Override
                        public void onError(
                                GetCredentialException e) {

                            runOnUiThread(() -> {

                                googleButton.setEnabled(true);

                                String message = e.getMessage();

                                if (message == null ||
                                        message.trim().isEmpty()) {

                                    message =
                                            "تعذر تسجيل الدخول باستخدام Google";
                                }

                                Toast.makeText(
                                        LoginActivity.this,
                                        message,
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
                    "تعذر تشغيل تسجيل الدخول باستخدام Google",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void handleGoogleCredential(
            GetCredentialResponse result) {

        googleButton.setEnabled(true);

        Credential credential = result.getCredential();

        if (!(credential instanceof CustomCredential)) {

            Toast.makeText(
                    this,
                    "لم يتم الحصول على بيانات حساب Google",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        CustomCredential customCredential =
                (CustomCredential) credential;

        if (!GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(customCredential.getType())) {

            Toast.makeText(
                    this,
                    "نوع بيانات Google غير معروف",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(
                            customCredential.getData()
                    );

            String idToken =
                    googleCredential.getIdToken();

            firebaseAuthWithGoogle(idToken);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "تعذر قراءة بيانات حساب Google",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

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

                                showError(
                                        task.getException(),
                                        "تعذر تسجيل الدخول باستخدام Google"
                                );
                            }
                        }
                );
    }

    private void showError(
            Exception exception,
            String defaultMessage) {

        String message = defaultMessage;

        if (exception != null &&
                exception.getMessage() != null &&
                !exception.getMessage().trim().isEmpty()) {

            message = exception.getMessage();
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

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
    }
