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

import java.security.SecureRandom;
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

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        teamNameEditText   = findViewById(R.id.teamName);
        videoGameSpinner   = findViewById(R.id.videoGameSpinner);
        teamRankSpinner    = findViewById(R.id.teamRankSpinner);
        teamLogoImageView  = findViewById(R.id.teamLogoImageView);
        createTeamButton   = findViewById(R.id.createTeamButton);

        // Video game spinner setup
        ArrayAdapter<CharSequence> videoGameAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.video_game_list,
                android.R.layout.simple_spinner_item
        );
        videoGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoGameSpinner.setAdapter(videoGameAdapter);

        // Rank spinner (populated on game select)
        ArrayAdapter<CharSequence> teamRankAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{}
        );
        teamRankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(teamRankAdapter);

        videoGameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
                updateRankSpinner(adapter.getItemAtPosition(pos).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> adapter) { }
        });

        createTeamButton.setOnClickListener(v -> {
            final String teamName  = teamNameEditText.getText().toString().trim();
            final String videoGame = videoGameSpinner.getSelectedItem().toString();
            final String teamRank  = teamRankSpinner.getSelectedItem().toString();

            if (teamName.isEmpty()
                    || "Select a game".equals(videoGame)
                    || "Select a rank".equals(teamRank)) {
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

            final String ownerUid  = user.getUid();
            final String ownerName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName()
                    : ownerUid;

            // Generate a 6-character join code
            final String joinCode = generateCode();

            // Create the team document with auto-ID
            DocumentReference teamRef = db
                    .collection("users")
                    .document(ownerUid)
                    .collection("teams")
                    .document();

            // Prepare metadata
            Map<String,Object> teamData = new HashMap<>();
            teamData.put("teamName", teamName);
            teamData.put("game",     videoGame);
            teamData.put("rank",     teamRank);
            teamData.put("joinCode", joinCode);
            teamData.put("ownerUid", ownerUid);

            // Write team doc
            teamRef.set(teamData)
                    .addOnSuccessListener(a -> {
                        // Add owner as member
                        Map<String,Object> ownerMember = new HashMap<>();
                        ownerMember.put("name", ownerName);
                        ownerMember.put("role", "Owner");

                        teamRef.collection("members")
                                .document(ownerUid)
                                .set(ownerMember)
                                .addOnSuccessListener(a2 -> {
                                    Toast.makeText(CreateTeamActivity.this,
                                            "Team created! Code: " + joinCode,
                                            Toast.LENGTH_LONG).show();
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

        teamLogoImageView.setOnClickListener(v ->
                Toast.makeText(this,
                        "Select Logo coming soon",
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void updateRankSpinner(String selectedGame) {
        int rankArray;
        switch (selectedGame) {
            case "League of Legends":
                rankArray = R.array.lol_rank_list; break;
            case "Apex Legends":
                rankArray = R.array.apex_rank_list; break;
            case "Valorant":
                rankArray = R.array.valorant_rank_list; break;
            case "CS2":
                rankArray = R.array.cs2_rank_list; break;
            case "Rocket League":
                rankArray = R.array.rocket_league_rank_list; break;
            default:
                rankArray = R.array.lol_rank_list;
                Toast.makeText(this, "Game not supported", Toast.LENGTH_SHORT).show();
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, rankArray, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(adapter);
    }

    /**
     * Generates a random uppercase alphanumeric code of length CODE_LENGTH.
     */
    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
