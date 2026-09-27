package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText emailEditText;
    private EditText passwordEditText;

    private Button loginButton;
    private Button registerButton;
    private Button googleButton;

    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            openMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleButton = findViewById(R.id.googleButton);

        credentialManager = CredentialManager.create(this);

        loginButton.setOnClickListener(v -> login());

        registerButton.setOnClickListener(v -> register());

        googleButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void login() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("أدخل البريد الإلكتروني");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("أدخل كلمة المرور");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        openMainActivity();

                    } else {

                        String message = "فشل تسجيل الدخول";

                        if (task.getException() != null) {
                            message += "\n" + task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void register() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("أدخل البريد الإلكتروني");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("أدخل كلمة المرور");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "تم إنشاء الحساب بنجاح",
                                Toast.LENGTH_LONG
                        ).show();

                        openMainActivity();

                    } else {

                        String message = "تعذر إنشاء الحساب";

                        if (task.getException() != null) {
                            message += "\n" + task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void signInWithGoogle() {

        Toast.makeText(
                this,
                "جاري فتح حساب Google...",
                Toast.LENGTH_SHORT
        ).show();

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id)
                        )
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {

                        runOnUiThread(() ->
                                handleGoogleCredential(result.getCredential())
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull androidx.credentials.exceptions.GetCredentialException e
                    ) {

                        runOnUiThread(() -> {

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
    }

    private void handleGoogleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {

                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData()
                            );

                    String idToken =
                            googleIdTokenCredential.getIdToken();

                    firebaseAuthWithGoogle(idToken);

                } catch (GoogleIdTokenParsingException e) {

                    Toast.makeText(
                            LoginActivity.this,
                            "تعذر قراءة حساب Google",
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {

                Toast.makeText(
                        LoginActivity.this,
                        "بيانات Google غير صحيحة",
                        Toast.LENGTH_LONG
                ).show();
            }

        } else {

            Toast.makeText(
                    LoginActivity.this,
                    "لم يتم اختيار حساب Google",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "تم تسجيل الدخول باستخدام Google",
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainActivity();

                    } else {

                        String message =
                                "فشل تسجيل الدخول باستخدام Google";

                        if (task.getException() != null) {
                            message += "\n" +
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

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
