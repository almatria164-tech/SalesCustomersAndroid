package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.security.SecureRandom;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        Button googleButton = findViewById(R.id.googleSignInButton);

        googleButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {

        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(
                        getString(R.string.default_web_client_id))
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
                getMainExecutor(),
                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        GetCredentialException>() {

                    @Override
                    public void onResult(
                            @NonNull GetCredentialResponse response) {

                        handleCredential(
                                response.getCredential());
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException e) {

                        String error =
                                e.getClass().getSimpleName();

                        if (e.getMessage() != null) {
                            error += "\n" + e.getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                "Google:\n" + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void handleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential
                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {

                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData());

                    firebaseAuthWithGoogle(
                            googleCredential.getIdToken());

                } catch (Exception e) {

                    Toast.makeText(
                            this,
                            "بيانات Google غير صالحة",
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {

                Toast.makeText(
                        this,
                        "نوع حساب Google غير مدعوم",
                        Toast.LENGTH_LONG
                ).show();
            }

        } else {

            Toast.makeText(
                    this,
                    "لم يتم الحصول على بيانات Google",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                startActivity(
                                        new Intent(
                                                this,
                                                MainActivity.class));

                                finish();

                            } else {

                                String error =
                                        "فشل تسجيل الدخول في Firebase";

                                if (task.getException() != null) {
                                    error += "\n"
                                            + task.getException()
                                            .getMessage();
                                }

                                Toast.makeText(
                                        this,
                                        error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
    }

    private String generateNonce() {

        byte[] nonce = new byte[32];

        new SecureRandom().nextBytes(nonce);

        return Base64.encodeToString(
                nonce,
                Base64.NO_WRAP
                        | Base64.URL_SAFE
                        | Base64.NO_PADDING);
    }
}
