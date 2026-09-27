package com.example.salescustomers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText emailEditText;
    private EditText passwordEditText;

    private Button loginButton;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> login());

        registerButton.setOnClickListener(v -> register());
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

                        startActivity(
                                new Intent(
                                        LoginActivity.this,
                                        MainActivity.class
                                )
                        );

                        finish();

                    } else {

                        Toast.makeText(
                                LoginActivity.this,
                                "فشل تسجيل الدخول",
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

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "تم إنشاء الحساب بنجاح",
                                Toast.LENGTH_LONG
                        ).show();

                        startActivity(
                                new Intent(
                                        LoginActivity.this,
                                        MainActivity.class
                                )
                        );

                        finish();

                    } else {

                        String message = "تعذر إنشاء الحساب";

                        if (task.getException() != null) {
                            message += "\n"
                                    + task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
