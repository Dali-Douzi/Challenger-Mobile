package com.example.challengermobile;

import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateTeamActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_team);

        // UI elements
        EditText teamNameEditText = findViewById(R.id.teamName);
        Spinner videoGameSpinner = findViewById(R.id.videoGameSpinner);
        Spinner teamRankSpinner = findViewById(R.id.teamRankSpinner);
        ImageView teamLogoImageView = findViewById(R.id.teamLogoImageView);
        Button createTeamButton = findViewById(R.id.createTeamButton);

        // Set up video game spinner with predefined games
        ArrayAdapter<CharSequence> videoGameAdapter = ArrayAdapter.createFromResource(this,
                R.array.video_game_list, android.R.layout.simple_spinner_item);
        videoGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoGameSpinner.setAdapter(videoGameAdapter);

        // Set up team rank spinner (initially empty)
        ArrayAdapter<CharSequence> teamRankAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{});  // Default empty ranks
        teamRankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(teamRankAdapter);

        // Listen for changes in video game selection
        videoGameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, android.view.View selectedItemView,
                                       int position, long id) {
                String selectedGame = parentView.getItemAtPosition(position).toString();
                updateRankSpinner(selectedGame, teamRankSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing when no selection is made
            }
        });

        // Create Team button functionality
        createTeamButton.setOnClickListener(v -> {
            String teamName = teamNameEditText.getText().toString();
            String videoGame = videoGameSpinner.getSelectedItem().toString();
            String teamRank = teamRankSpinner.getSelectedItem().toString();

            if (teamName.isEmpty() || videoGame.equals("Select a game") || teamRank.equals("Select a rank")) {
                Toast.makeText(CreateTeamActivity.this, "Please fill all details", Toast.LENGTH_SHORT).show();
            } else {
                // Here you can save the team data to Firebase
                // For now, we show a toast message
                Toast.makeText(CreateTeamActivity.this, "Team created successfully!", Toast.LENGTH_SHORT).show();
                finish();  // Close the activity after team creation
            }
        });

        // Logic to select team logo (optional file picker)
        teamLogoImageView.setOnClickListener(v -> {
            // Implement logo selection functionality (open file picker or gallery)
            Toast.makeText(this, "Select Logo functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateRankSpinner(String selectedGame, Spinner teamRankSpinner) {
        int rankArrayResource = 0;
        switch (selectedGame) {
            case "League of Legends":
                rankArrayResource = R.array.lol_rank_list;
                break;
            case "Apex Legends":
                rankArrayResource = R.array.apex_rank_list;
                break;
            case "Valorant":
                rankArrayResource = R.array.valorant_rank_list;
                break;
            case "CS2":
                rankArrayResource = R.array.cs2_rank_list;
                break;
            case "Rocket League":
                rankArrayResource = R.array.rocket_league_rank_list;
                break;
            default:
                rankArrayResource = R.array.lol_rank_list;  // Default to LoL ranks
                Toast.makeText(this, "Game not supported", Toast.LENGTH_SHORT).show();
        }

        // Update the team rank spinner with the appropriate ranks for the selected game
        ArrayAdapter<CharSequence> teamRankAdapter = ArrayAdapter.createFromResource(this,
                rankArrayResource, android.R.layout.simple_spinner_item);
        teamRankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankSpinner.setAdapter(teamRankAdapter);
    }
}
