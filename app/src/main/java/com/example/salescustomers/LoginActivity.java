package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        Button googleButton = findViewById(R.id.googleSignInButton);

        googleButton.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);

                AuthCredential credential = GoogleAuthProvider.getCredential(
                        account.getIdToken(), null);

                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this,
                                        "فشل تسجيل الدخول",
                                        Toast.LENGTH_LONG).show();
                            }
                        });

            } catch (ApiException e) {
                Toast.makeText(this,
                        "فشل تسجيل الدخول إلى Google",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
