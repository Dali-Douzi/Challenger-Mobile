package com.example.challengermobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PostScrimActivity extends AppCompatActivity {
    public static final String EXTRA_TEAM_ID = "team_id";

    private FirebaseFirestore db;
    private String teamId;
    private EditText scrimDateEt, scrimTimeEt;
    private Button postScrimBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_scrim);

        db = FirebaseFirestore.getInstance();
        teamId = getIntent().getStringExtra(EXTRA_TEAM_ID);

        scrimDateEt = findViewById(R.id.scrimDate);
        scrimTimeEt = findViewById(R.id.scrimTime);
        postScrimBtn = findViewById(R.id.postScrimButton);

        postScrimBtn.setOnClickListener(v -> postScrim());
    }

    private void postScrim() {
        String dateStr = scrimDateEt.getText().toString().trim();
        String timeStr = scrimTimeEt.getText().toString().trim();
        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Please enter both date and time", Toast.LENGTH_SHORT).show();
            return;
        }
        // parse into Date
        Date dateTime;
        try {
            dateTime = new SimpleDateFormat("MM/dd/yyyy HH:mm").parse(dateStr + " " + timeStr);
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date/time format", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // fetch team details
        String uid = user.getUid();
        CollectionReference teamsRef = db.collection("users").document(uid).collection("teams");
        teamsRef.document(teamId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Team not found", Toast.LENGTH_SHORT).show();
                return;
            }
            String teamName = doc.getString("teamName");
            String game     = doc.getString("game");
            String rank     = doc.getString("rank");

            // build scrim object
            Map<String,Object> scrim = new HashMap<>();
            scrim.put("teamA", teamName);
            scrim.put("teamB", "");            // to be filled by other team
            scrim.put("scrimDate", dateTime);
            scrim.put("status", "Pending");
            scrim.put("game", game);
            scrim.put("rank", rank);

            // save under top-level 'scrims' collection
            db.collection("scrims")
                    .add(scrim)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Scrim posted!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }
}
