package com.example.challengermobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    private RecyclerView      scrimRecyclerView;
    private ScrimAdapter      scrimAdapter;
    private List<Scrim>       scrimList    = new ArrayList<>();
    private Button            postScrimButton;
    private List<String>      userTeamIds  = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scrimRecyclerView = findViewById(R.id.scrimRecyclerView);
        scrimRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        scrimAdapter = new ScrimAdapter(scrimList, userTeamIds);
        scrimRecyclerView.setAdapter(scrimAdapter);

        postScrimButton = findViewById(R.id.postScrimButton);
        postScrimButton.setOnClickListener(v ->
                startActivity(new Intent(this, PostScrimActivity.class))
        );

        setupScrimListener();
        fetchUserTeamIds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserTeamIds();
    }

    private void fetchUserTeamIds() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("teams")
                .get()
                .addOnSuccessListener(qs -> {
                    userTeamIds.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        userTeamIds.add(doc.getId());
                    }
                    scrimAdapter.setUserTeamIds(userTeamIds);
                    scrimAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load your teams: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void setupScrimListener() {
        db.collection("scrims")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                "Error loading scrims: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Scrim> updated = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String id        = doc.getId();
                        String teamId    = doc.getString("teamId");
                        String teamName  = doc.getString("teamName");
                        String teamRank  = doc.get("teamRank") != null
                                ? doc.get("teamRank").toString() : "";
                        Date   timestamp = doc.getDate("timestamp");
                        String format    = doc.getString("format");
                        String status    = doc.getString("status");

                        Scrim s = new Scrim(id, teamId, teamName, teamRank, timestamp, format);
                        s.setStatus(status);

                        Object pr = doc.get("pendingRequests");
                        if (pr instanceof List) {
                            List<String> pending = new ArrayList<>();
                            for (Object o : (List<?>) pr) {
                                if (o != null) pending.add(o.toString());
                            }
                            s.setPendingRequests(pending);
                        }
                        updated.add(s);
                    }
                    scrimAdapter.updateList(updated);
                });
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
                    .addOnSuccessListener(qs -> {
                        int order = 3;  // after Profile (1) & Create (2)
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            menu.add(
                                    R.id.group_teams,
                                    Menu.NONE,
                                    order++,
                                    doc.getString("teamName")
                            ).setIntent(new Intent(this, TeamDashboardActivity.class)
                                    .putExtra(TeamDashboardActivity.EXTRA_TEAM_ID, doc.getId()));
                        }
                    });
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_notifications) {
            startActivity(new Intent(this, NotificationsActivity.class));
            return true;
        }
        if (item.getGroupId() == R.id.group_teams) {
            String teamId = item.getIntent()
                    .getStringExtra(TeamDashboardActivity.EXTRA_TEAM_ID);
            getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("currentTeamId", teamId)
                    .apply();
            startActivity(item.getIntent());
            return true;
        }
        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (id == R.id.menu_create_team) {
            startActivity(new Intent(this, CreateTeamActivity.class));
            return true;
        }
        if (id == R.id.menu_join_team) {
            showJoinTeamDialog();
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

    private void showJoinTeamDialog() {
        EditText et = new EditText(this);
        et.setHint("Team Code");
        et.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Join Team")
                .setView(et)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = et.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 1) Find team by joinCode
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
                                        .getParent()  // ./teams
                                        .getParent()  // ./users/{ownerUid}
                                        .getId();
                                String teamId   = t.getReference().getId();
                                String teamName = t.getString("teamName");
                                String game     = t.getString("game");
                                String rank     = t.getString("rank");
                                String logoUrl  = t.getString("logoUrl");

                                String me = mAuth.getCurrentUser().getUid();

                                // 2) Prepare metadata
                                Map<String,Object> data = new HashMap<>();
                                data.put("teamName", teamName);
                                data.put("game",     game);
                                data.put("rank",     rank);
                                data.put("joinCode", code);
                                data.put("ownerUid", ownerUid);
                                if (logoUrl != null) {
                                    data.put("logoUrl", logoUrl);
                                }

                                // 3) Write under my teams/{teamId}
                                db.collection("users")
                                        .document(me)
                                        .collection("teams")
                                        .document(teamId)
                                        .set(data)
                                        .addOnSuccessListener(a -> {
                                            // 4) Add membership under owner’s subcollection
                                            Map<String,Object> member = new HashMap<>();
                                            String display = mAuth.getCurrentUser().getDisplayName();
                                            if (display == null || display.isEmpty()) display = me;
                                            member.put("name", display);
                                            member.put("role", "Member");

                                            t.getReference()
                                                    .collection("members")
                                                    .document(me)
                                                    .set(member)
                                                    .addOnSuccessListener(a2 -> {
                                                        Toast.makeText(this,
                                                                "Joined " + teamName,
                                                                Toast.LENGTH_SHORT).show();
                                                        invalidateOptionsMenu();
                                                    })
                                                    .addOnFailureListener(e2 -> {
                                                        Toast.makeText(this,
                                                                "Error adding member: " + e2.getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    });
                                        })
                                        .addOnFailureListener(e1 -> {
                                            Toast.makeText(this,
                                                    "Error joining team: " + e1.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
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
