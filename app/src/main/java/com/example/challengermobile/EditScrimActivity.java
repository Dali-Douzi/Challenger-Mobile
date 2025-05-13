package com.example.challengermobile;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class EditScrimActivity extends AppCompatActivity {

    public static final String EXTRA_SCRIM_ID = "EXTRA_SCRIM_ID";

    private Spinner spinnerTeam, spinnerDay, spinnerTime, spinnerFormat;
    private Button  btnValidate, btnDelete;
    private FirebaseFirestore db;
    private String scrimId;
    private String teamId, teamName;

    private List<String> dayOptions, timeOptions;
    private String[] formatOptions = {
            "1 game", "2 games", "3 games", "4 games", "5 games",
            "Best of 2", "Best of 3", "Best of 5"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_scrim);

        db            = FirebaseFirestore.getInstance();
        spinnerTeam   = findViewById(R.id.spinnerTeam);
        spinnerDay    = findViewById(R.id.spinnerDay);
        spinnerTime   = findViewById(R.id.spinnerTime);
        spinnerFormat = findViewById(R.id.spinnerFormat);
        btnValidate   = findViewById(R.id.btnValidate);
        btnDelete     = findViewById(R.id.btnDelete);

        scrimId = getIntent().getStringExtra(EXTRA_SCRIM_ID);

        initDayOptions();
        initTimeOptions();
        spinnerDay.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, dayOptions));
        spinnerTime.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, timeOptions));
        spinnerFormat.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, formatOptions));

        db.collection("scrims")
                .document(scrimId)
                .get()
                .addOnSuccessListener(this::populateFields)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load scrim: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

        btnValidate.setOnClickListener(v -> updateScrim());
        btnDelete  .setOnClickListener(v -> confirmDelete());
    }

    private void initDayOptions() {
        dayOptions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "MMM d", getResources().getConfiguration().locale
        );
        dayOptions.add("Today");
        dayOptions.add("Tomorrow");
        for (int i = 2; i < 9; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            dayOptions.add(sdf.format(cal.getTime()));
        }
    }

    private void initTimeOptions() {
        timeOptions = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 30) {
                timeOptions.add(String.format("%02d:%02d", h, m));
            }
        }
    }

    private void populateFields(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "Scrim no longer exists", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        teamId    = doc.getString("teamId");
        teamName  = doc.getString("teamName");
        Date timestamp = doc.getDate("timestamp");
        String format  = doc.getString("format");

        ArrayList<String> teams = new ArrayList<>();
        teams.add(teamName);
        spinnerTeam.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, teams));

        selectDay(timestamp);
        selectTime(timestamp);

        for (int i = 0; i < formatOptions.length; i++) {
            if (formatOptions[i].equals(format)) {
                spinnerFormat.setSelection(i);
                break;
            }
        }
    }

    private void selectDay(Date when) {
        Calendar then = Calendar.getInstance();
        then.setTime(when);
        Calendar now = Calendar.getInstance();

        long diff = then.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
        if (diff == 0) spinnerDay.setSelection(0);
        else if (diff == 1) spinnerDay.setSelection(1);
        else {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "MMM d", getResources().getConfiguration().locale
            );
            String label = sdf.format(when);
            int idx = dayOptions.indexOf(label);
            if (idx >= 0) spinnerDay.setSelection(idx);
        }
    }

    private void selectTime(Date when) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String label = sdf.format(when);
        int idx = timeOptions.indexOf(label);
        if (idx >= 0) spinnerTime.setSelection(idx);
    }

    private void updateScrim() {
        String dayStr  = spinnerDay.getSelectedItem().toString();
        String timeStr = spinnerTime.getSelectedItem().toString();

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
                        "MMM d", getResources().getConfiguration().locale
                ).parse(dayStr);
                pick.setTime(d);
            } catch (Exception ignored) {}
        }
        String[] parts = timeStr.split(":");
        pick.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        pick.set(Calendar.MINUTE,      Integer.parseInt(parts[1]));

        Map<String,Object> update = new HashMap<>();
        update.put("timestamp", pick.getTime());
        update.put("format",    spinnerFormat.getSelectedItem().toString());

        db.collection("scrims")
                .document(scrimId)
                .update(update)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Scrim updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete scrim?")
                .setMessage("This will remove the scrim permanently.")
                .setPositiveButton("Delete",
                        (DialogInterface d, int which) -> deleteScrim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteScrim() {
        db.collection("scrims")
                .document(scrimId)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Scrim deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
