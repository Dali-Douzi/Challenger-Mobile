package com.example.challengermobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Objects;


public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Set up the Toolbar as the ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);  // Ensure this is androidx.appcompat.widget.Toolbar
        setSupportActionBar(toolbar);  // This will make the Toolbar act as the ActionBar

        // Setting up buttons (Request Scrim and Post Scrim)
        Button requestScrimButton = findViewById(R.id.requestScrimButton);
        Button postScrimButton = findViewById(R.id.postScrimButton);

        // Request Scrim button functionality
        requestScrimButton.setOnClickListener(v -> {
            // Placeholder for Request Scrim logic
            Toast.makeText(DashboardActivity.this, "Request Scrim clicked", Toast.LENGTH_SHORT).show();
        });

        // Post Scrim button functionality
        postScrimButton.setOnClickListener(v -> {
            // Placeholder for Post Scrim logic
            Toast.makeText(DashboardActivity.this, "Post Scrim clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            List<String> userTeams = getUserTeams();

            for (String team : userTeams) {
                menu.add(Menu.NONE, Menu.NONE, Menu.NONE, team);
            }
        }
        return true;
    }

    private void getUserTeams() {
        // Get the current user's ID
        String userId = mAuth.getCurrentUser().getUid();

        // Reference to the teams collection for the current user
        CollectionReference teamsRef = db.collection("users").document(userId).collection("teams");

        teamsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    userTeams = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String teamName = document.getString("teamName");  // Assuming each team has a field "teamName"
                        userTeams.add(teamName);
                    }

                    // Update the UI (e.g., populate the Spinner or RecyclerView with the user's teams)
                    updateUIWithTeams();
                } else {
                    Toast.makeText(DashboardActivity.this, "No teams found for this user", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(DashboardActivity.this, "Error fetching teams: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String selectedTeam = Objects.requireNonNull(item.getTitle()).toString();

        if (item.getItemId() == R.id.menu_create_team) {
            startActivity(new Intent(DashboardActivity.this, CreateTeamActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
            finish();
            return true;
        } else {
            Intent intent = new Intent(DashboardActivity.this, PostScrimActivity.class);
            intent.putExtra("selected_team", selectedTeam);
            startActivity(intent);
            return true;
        }
    }
}
