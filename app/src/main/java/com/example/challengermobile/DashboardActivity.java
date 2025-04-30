// DashboardActivity.java
package com.example.challengermobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth    mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.removeGroup(R.id.group_teams);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("teams")
                    .get()
                    .addOnSuccessListener((QuerySnapshot qs) -> {
                        int order = 20;
                        menu.removeGroup(R.id.group_teams);
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            String name = doc.getString("teamName");
                            String id   = doc.getId();
                            Intent intent = new Intent(this, TeamDashboardActivity.class)
                                    .putExtra(TeamDashboardActivity.EXTRA_TEAM_ID, id);

                            menu.add(
                                    R.id.group_teams,
                                    Menu.NONE,
                                    order++,
                                    name
                            ).setIntent(intent);
                        }
                    });
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int mid = item.getItemId();
        if (mid == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (mid == R.id.menu_create_team) {
            startActivity(new Intent(this, CreateTeamActivity.class));
            return true;
        }
        if (mid == R.id.menu_join_team) {
            showJoinTeamDialog();
            return true;
        }
        if (mid == R.id.menu_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        // Dynamic team items
        if (item.getGroupId() == R.id.group_teams) {
            startActivity(item.getIntent());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showJoinTeamDialog() {
        EditText et = new EditText(this);
        et.setHint("Team Code");
        et.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Join Team")
                .setView(et)
                .setPositiveButton("Join", (d, w) -> {
                    String code = et.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collectionGroup("teams")
                            .whereEqualTo("joinCode", code)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (qs.isEmpty()) {
                                    Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                DocumentSnapshot t = qs.getDocuments().get(0);
                                String ownerUid = t.getReference()
                                        .getParent().getParent().getId();
                                String teamName = t.getString("teamName");
                                String game     = t.getString("game");
                                String rank     = t.getString("rank");

                                String me = mAuth.getCurrentUser().getUid();
                                Map<String,Object> data = new HashMap<>();
                                data.put("teamName", teamName);
                                data.put("game",     game);
                                data.put("rank",     rank);
                                data.put("joinCode", code);
                                data.put("ownerUid", ownerUid);

                                // Add to my teams
                                db.collection("users")
                                        .document(me)
                                        .collection("teams")
                                        .add(data);

                                // Mark me as a member under the original owner’s path
                                Map<String,Object> member = new HashMap<>();
                                String display = mAuth.getCurrentUser().getDisplayName();
                                if (display == null) display = me;
                                member.put("name", display);
                                member.put("role", "Player");
                                t.getReference()
                                        .collection("members")
                                        .add(member);

                                Toast.makeText(this,
                                        "Joined " + teamName,
                                        Toast.LENGTH_SHORT).show();
                                invalidateOptionsMenu();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
