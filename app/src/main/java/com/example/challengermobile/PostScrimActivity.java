package com.example.challengermobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostScrimActivity extends AppCompatActivity {

    private Spinner spinnerTeam, spinnerDay, spinnerTime, spinnerFormat;
    private Button  btnValidate;
    private FirebaseFirestore db;

    private List<String> teamIds         = new ArrayList<>();
    private List<String> teamNames       = new ArrayList<>();
    private List<String> teamRankStrings = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_scrim);

        db            = FirebaseFirestore.getInstance();
        spinnerTeam   = findViewById(R.id.spinnerTeam);
        spinnerDay    = findViewById(R.id.spinnerDay);
        spinnerTime   = findViewById(R.id.spinnerTime);
        spinnerFormat = findViewById(R.id.spinnerFormat);
        btnValidate   = findViewById(R.id.btnValidate);

        loadUserTeams();
        setupDaySpinner();
        setupTimeSpinner();
        setupFormatSpinner();

        btnValidate.setOnClickListener(v -> createScrim());
    }

    private void loadUserTeams() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .collection("teams")
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        Toast.makeText(this,
                                "You have no teams to post a scrim on.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : qs) {
                        teamIds.add(doc.getId());
                        teamNames.add(doc.getString("teamName"));
                        String rank = doc.getString("rank");
                        teamRankStrings.add(rank != null ? rank : "");
                    }
                    spinnerTeam.setAdapter(new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            teamNames
                    ));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load teams: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void setupDaySpinner() {
        List<String> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "MMM d",
                getResources().getConfiguration().locale
        );
        days.add("Today");
        days.add("Tomorrow");
        for (int i = 2; i < 9; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            days.add(sdf.format(cal.getTime()));
        }
        spinnerDay.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                days
        ));
    }

    private void setupTimeSpinner() {
        List<String> times = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                times.add(String.format("%02d:%02d", h, m));
            }
        }
        spinnerTime.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                times
        ));
    }

    private void setupFormatSpinner() {
        String[] formats = {
                "1 game", "2 games", "3 games",
                "4 games", "5 games",
                "Best of 2", "Best of 3", "Best of 5"
        };
        spinnerFormat.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                formats
        ));
    }

    private void createScrim() {
        int    idx       = spinnerTeam.getSelectedItemPosition();
        String dayStr    = spinnerDay.getSelectedItem().toString();
        String timeStr   = spinnerTime.getSelectedItem().toString();
        String formatStr = spinnerFormat.getSelectedItem().toString();

        // Build the scheduled Date
        Calendar pick = Calendar.getInstance();
        pick.set(Calendar.HOUR_OF_DAY, 0);
        pick.set(Calendar.MINUTE,      0);
        pick.set(Calendar.SECOND,      0);
        pick.set(Calendar.MILLISECOND, 0);
        if ("Tomorrow".equals(dayStr)) {
            pick.add(Calendar.DAY_OF_YEAR, 1);
        } else if (!"Today".equals(dayStr)) {
            try {
                Date d = new SimpleDateFormat(
                        "MMM d",
                        getResources().getConfiguration().locale
                ).parse(dayStr);
                pick.setTime(d);
            } catch (Exception ignored) {}
        }
        String[] parts = timeStr.split(":");
        pick.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        pick.set(Calendar.MINUTE,      Integer.parseInt(parts[1]));

        Date scheduled = pick.getTime();

        // Gather team info
        String teamId   = teamIds.get(idx);
        String teamName = teamNames.get(idx);
        String teamRank = teamRankStrings.get(idx);

        // Persist this as the active team
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("currentTeamId", teamId)
                .apply();

        // Prepare a Map<String,Object> for Firestore
        String docId = db.collection("scrims").document().getId();
        Map<String,Object> data = new HashMap<>();
        data.put("teamId",           teamId);
        data.put("teamName",         teamName);
        data.put("ownerUid",       FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("teamRank",         teamRank);
        data.put("timestamp",        scheduled);
        data.put("format",           formatStr);
        data.put("status",           "open");
        data.put("pendingRequests",  new ArrayList<String>());

        // Write to Firestore
        db.collection("scrims")
                .document(docId)
                .set(data)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Scrim posted!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to post scrim: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
