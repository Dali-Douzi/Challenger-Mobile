package com.example.challengermobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PostScrimActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_scrim);

        // Get the selected team passed from DashboardActivity
        Intent intent = getIntent();
        String selectedTeam = intent.getStringExtra("selected_team");

        // Show the selected team (could be used to display in the UI)
        Toast.makeText(this, "Selected Team: " + selectedTeam, Toast.LENGTH_SHORT).show();

        // UI elements for entering scrim details
        EditText scrimTime = findViewById(R.id.scrimTime);
        EditText scrimDate = findViewById(R.id.scrimDate);
        Button postScrimButton = findViewById(R.id.postScrimButton);

        // Post Scrim button functionality
        postScrimButton.setOnClickListener(v -> {
            String time = scrimTime.getText().toString();
            String date = scrimDate.getText().toString();

            if (time.isEmpty() || date.isEmpty()) {
                Toast.makeText(PostScrimActivity.this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                // Logic to post scrim (e.g., save to Firebase)
                Toast.makeText(PostScrimActivity.this, "Scrim posted successfully!", Toast.LENGTH_SHORT).show();
                finish();  // Close the activity after posting the scrim
            }
        });
    }
}
