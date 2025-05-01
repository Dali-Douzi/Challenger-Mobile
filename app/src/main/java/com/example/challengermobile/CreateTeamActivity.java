package com.example.challengermobile;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateTeamActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText teamNameEditText;
    private Spinner videoGameSpinner;
    private Spinner teamRankSpinner;
    private ImageView teamLogoImageView;
    private Button createTeamButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        // Initialize FirebaseAuth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Wire up views
        teamNameEditText   = findViewById(R.id.teamName);
        videoGameSpinner   = findViewById(R.id.videoGameSpinner);
        teamRankSpinner    = findViewById(R.id.teamRankSpinner);
        teamLogoImageView  = findViewById(R.id.teamLogoImageView);
        createTeamButton   = findViewById(R.id.createTeamButton);

        // Set up game spinner
        ArrayAdapter<CharSequence> videoGameAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.video_game_list,
                android.R.layout.simple_spinner_item
        );
        videoGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoGameSpinner.setAdapter(videoGameAdapter);

        // Set up empty rank spinner
        ArrayAdapter<CharSequence> teamRankAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{}
        );
        teamRankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(teamRankAdapter);

        // Populate rank options when a game is selected
        videoGameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
                String selectedGame = adapter.getItemAtPosition(pos).toString();
                updateRankSpinner(selectedGame);
            }
            @Override public void onNothingSelected(AdapterView<?> adapter) { }
        });

        // Handle "Create Team" click
        createTeamButton.setOnClickListener(v -> {
            final String teamName  = teamNameEditText.getText().toString().trim();
            final String videoGame = videoGameSpinner.getSelectedItem().toString();
            final String teamRank  = teamRankSpinner.getSelectedItem().toString();

            if (teamName.isEmpty()
                    || videoGame.equals("Select a game")
                    || teamRank.equals("Select a rank")) {
                Toast.makeText(this,
                        "Please fill all details",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this,
                        "User not signed in",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Compute owner UID & name as final variables
            final String ownerUid  = user.getUid();
            final String ownerName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : ownerUid;

            // Create team document under users/{ownerUid}/teams/{teamId}
            DocumentReference teamRef = db.collection("users")
                    .document(ownerUid)
                    .collection("teams")
                    .document();
            final String teamId = teamRef.getId();

            // Prepare team metadata
            Map<String,Object> teamData = new HashMap<>();
            teamData.put("teamName", teamName);
            teamData.put("game",     videoGame);
            teamData.put("rank",     teamRank);
            teamData.put("joinCode", teamId);
            teamData.put("ownerUid", ownerUid);

            // Write the team document
            teamRef.set(teamData)
                    .addOnSuccessListener(a -> {
                        // Immediately add the owner as a real member
                        Map<String,Object> ownerMember = new HashMap<>();
                        ownerMember.put("name", ownerName);
                        ownerMember.put("role", "Owner");

                        teamRef.collection("members")
                                .document(ownerUid)
                                .set(ownerMember)
                                .addOnSuccessListener(a2 -> {
                                    Toast.makeText(CreateTeamActivity.this,
                                            "Team created successfully!",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e2 -> {
                                    Toast.makeText(CreateTeamActivity.this,
                                            "Failed to add owner as member: " + e2.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateTeamActivity.this,
                                "Failed to create team: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // Stub for logo selection
        teamLogoImageView.setOnClickListener(v ->
                Toast.makeText(this,
                        "Select Logo functionality coming soon",
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void updateRankSpinner(String selectedGame) {
        int rankArray;
        switch (selectedGame) {
            case "League of Legends":
                rankArray = R.array.lol_rank_list;
                break;
            case "Apex Legends":
                rankArray = R.array.apex_rank_list;
                break;
            case "Valorant":
                rankArray = R.array.valorant_rank_list;
                break;
            case "CS2":
                rankArray = R.array.cs2_rank_list;
                break;
            case "Rocket League":
                rankArray = R.array.rocket_league_rank_list;
                break;
            default:
                rankArray = R.array.lol_rank_list;
                Toast.makeText(this,
                        "Game not supported",
                        Toast.LENGTH_SHORT).show();
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                rankArray,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(adapter);
    }
}
