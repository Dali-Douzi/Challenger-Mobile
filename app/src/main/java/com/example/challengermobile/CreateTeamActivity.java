package com.example.challengermobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateTeamActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Spinner videoGameSpinner;
    private Spinner teamRankSpinner;
    private EditText teamNameEditText;
    private Button createTeamButton;
    private ImageView teamLogoImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Create Team");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // UI refs
        teamNameEditText     = findViewById(R.id.teamName);
        videoGameSpinner     = findViewById(R.id.videoGameSpinner);
        teamRankSpinner      = findViewById(R.id.teamRankSpinner);
        teamLogoImageView    = findViewById(R.id.teamLogoImageView);
        createTeamButton     = findViewById(R.id.createTeamButton);

        // Video game spinner
        ArrayAdapter<CharSequence> videoGameAdapter = ArrayAdapter.createFromResource(
                this, R.array.video_game_list, android.R.layout.simple_spinner_item
        );
        videoGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoGameSpinner.setAdapter(videoGameAdapter);

        // Team rank spinner initially empty
        teamRankSpinner.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{})
        );

        // Populate rank spinner when game selected
        videoGameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                updateRankSpinner(parent.getItemAtPosition(pos).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Create team
        createTeamButton.setOnClickListener(v -> {
            String name = teamNameEditText.getText().toString().trim();
            String game = videoGameSpinner.getSelectedItem().toString();
            String rank = teamRankSpinner.getSelectedItem().toString();
            if (name.isEmpty() || rank.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate a random 8-char code
            String joinCode = UUID.randomUUID().toString().substring(0, 8);

            // Build team data
            Map<String,Object> team = new HashMap<>();
            team.put("teamName", name);
            team.put("game", game);
            team.put("rank", rank);
            team.put("joinCode", joinCode);
            team.put("ownerUid", user.getUid());

            // Save to Firestore
            db.collection("users")
                    .document(user.getUid())
                    .collection("teams")
                    .add(team)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Team created! Code: " + joinCode, Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        // Team logo picker stub
        teamLogoImageView.setOnClickListener(v ->
                Toast.makeText(this, "Select Logo functionality coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateRankSpinner(String selectedGame) {
        int resId;
        switch (selectedGame) {
            case "League of Legends": resId = R.array.lol_rank_list; break;
            case "Apex Legends":      resId = R.array.apex_rank_list; break;
            case "Valorant":          resId = R.array.valorant_rank_list; break;
            case "CS2":               resId = R.array.cs2_rank_list; break;
            case "Rocket League":     resId = R.array.rocket_league_rank_list; break;
            default:
                resId = R.array.team_rank_list;
                Toast.makeText(this, "Game not supported", Toast.LENGTH_SHORT).show();
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, resId, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (id == R.id.menu_create_team) {
            return true;
        }
        if (id == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
