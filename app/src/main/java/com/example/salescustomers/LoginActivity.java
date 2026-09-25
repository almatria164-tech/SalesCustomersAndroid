package com.example.salescustomers;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private static final String GOOGLE_ID_TOKEN_TYPE =
            "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL";

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

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id))
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new androidx.credentials.CredentialManagerCallback<Credential,
                        GetCredentialException>() {

                    @Override
                    public void onResult(@NonNull Credential credential) {
                        handleCredential(credential);
                    }

                    @Override
                    public void onError(
                            @NonNull GetCredentialException e) {

                        Toast.makeText(
                                LoginActivity.this,
                                "فشل تسجيل الدخول إلى Google",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void handleCredential(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GOOGLE_ID_TOKEN_TYPE.equals(customCredential.getType())) {

                try {
                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData());

                    firebaseAuthWithGoogle(
                            googleCredential.getIdToken());

                } catch (GoogleIdTokenParsingException e) {

                    Toast.makeText(
                            this,
                            "تعذر قراءة حساب Google",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        startActivity(
                                new android.content.Intent(
                                        this,
                                        MainActivity.class));

                        finish();

                    } else {

                        Toast.makeText(
                                this,
                                "فشل تسجيل الدخول",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
