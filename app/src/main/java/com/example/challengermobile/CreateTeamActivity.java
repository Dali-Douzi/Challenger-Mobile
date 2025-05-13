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

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    private EditText  teamNameEditText;
    private Spinner   videoGameSpinner;
    private Spinner   teamRankSpinner;
    private ImageView teamLogoImageView;
    private Button    createTeamButton;

    private static final String CODE_CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int    CODE_LENGTH = 6;
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

        // Populate spinners...
        ArrayAdapter<CharSequence> gameAdapter = ArrayAdapter.createFromResource(
                this, R.array.video_game_list, android.R.layout.simple_spinner_item
        );
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoGameSpinner.setAdapter(gameAdapter);

        videoGameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                updateRankSpinner(parent.getItemAtPosition(pos).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        createTeamButton.setOnClickListener(v -> createTeam());
        teamLogoImageView.setOnClickListener(v ->
                Toast.makeText(this, "Logo picker coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void createTeam() {
        String name  = teamNameEditText.getText().toString().trim();
        String game  = videoGameSpinner.getSelectedItem().toString();
        String rank  = teamRankSpinner.getSelectedItem().toString();
        if (name.isEmpty() || "Select a game".equals(game) || "Select a rank".equals(rank)) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String ownerUid  = user.getUid();
        String ownerName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : ownerUid;
        String joinCode  = generateCode();

        // 1) User‐scoped reference
        DocumentReference userTeamRef = db
                .collection("users")
                .document(ownerUid)
                .collection("teams")
                .document();
        // 2) Global teams reference
        String teamId = userTeamRef.getId();
        DocumentReference globalTeamRef = db
                .collection("teams")
                .document(teamId);

        Map<String,Object> data = new HashMap<>();
        data.put("teamName", name);
        data.put("game",     game);
        data.put("rank",     rank);
        data.put("joinCode", joinCode);
        data.put("ownerUid", ownerUid);

        // Write both
        userTeamRef.set(data)
                .addOnSuccessListener(a -> globalTeamRef.set(data)
                        .addOnSuccessListener(b -> {
                            // Add owner as member in both
                            Map<String,Object> ownerMember = new HashMap<>();
                            ownerMember.put("name", ownerName);
                            ownerMember.put("role", "Owner");

                            userTeamRef.collection("members")
                                    .document(ownerUid)
                                    .set(ownerMember);

                            globalTeamRef.collection("members")
                                    .document(ownerUid)
                                    .set(ownerMember)
                                    .addOnSuccessListener(c -> {
                                        Toast.makeText(
                                                CreateTeamActivity.this,
                                                "Team created! Code: " + joinCode,
                                                Toast.LENGTH_LONG
                                        ).show();
                                        finish();
                                    });
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(
                                CreateTeamActivity.this,
                                "Create failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void updateRankSpinner(String game) {
        int arrayId;
        switch (game) {
            case "League of Legends": arrayId = R.array.lol_rank_list; break;
            case "Apex Legends":      arrayId = R.array.apex_rank_list; break;
            case "Valorant":          arrayId = R.array.valorant_rank_list; break;
            case "CS2":               arrayId = R.array.cs2_rank_list; break;
            case "Rocket League":     arrayId = R.array.rocket_league_rank_list; break;
            default:                  arrayId = R.array.team_rank_list;
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayId, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(adapter);
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
