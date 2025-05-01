// RegisterActivity.java
package com.example.challengermobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        EditText usernameEditText       = findViewById(R.id.usernameEditText);
        EditText emailEditText          = findViewById(R.id.emailEditText);
        EditText passwordEditText       = findViewById(R.id.passwordEditText);
        EditText confirmPasswordEditText= findViewById(R.id.confirmPasswordEditText);
        Button registerButton           = findViewById(R.id.registerButton);
        Button loginButton              = findViewById(R.id.loginButton);

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String email    = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirm  = confirmPasswordEditText.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            } else if (email.isEmpty() || password.isEmpty() || !password.equals(confirm)) {
                Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(username, email, password);
            }
        });

        loginButton.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class))
        );
    }

    private void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 1) Update FirebaseAuth profile
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest prof = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                            user.updateProfile(prof);
                        }

                        // 2) Persist in Firestore under "users" collection
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String,Object> data = new HashMap<>();
                        data.put("username", username);
                        data.put("email", email);
                        db.collection("users")
                                .document(uid)
                                .set(data);

                        // 3) Notify and navigate
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
