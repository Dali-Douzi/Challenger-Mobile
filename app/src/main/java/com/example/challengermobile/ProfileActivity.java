package com.example.challengermobile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Set up profile views (e.g., display user data)
        TextView profileInfo = findViewById(R.id.profileInfo);
        // Display user info (this can be fetched from Firestore or FirebaseAuth)
        profileInfo.setText("User Profile Information");
    }
}
