package com.example.challengermobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TeamDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID = "team_id";

    private FirebaseFirestore db;
    private String teamId;
    private String ownerUid;
    private boolean isOwner;

    private TextView tvTeamName, tvGame, tvRank, tvJoinCode;
    private ImageView ivTeamLogo;
    private RecyclerView rvMembers;
    private Button btnCopyCode, btnDeleteTeam;
    private MembersAdapter membersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_dashboard);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Team Details");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Firestore + Intent data
        db     = FirebaseFirestore.getInstance();
        teamId = getIntent().getStringExtra(EXTRA_TEAM_ID);

        // Views
        tvTeamName    = findViewById(R.id.tvTeamName);
        tvGame        = findViewById(R.id.tvGame);
        tvRank        = findViewById(R.id.tvRank);
        ivTeamLogo    = findViewById(R.id.ivTeamLogo);
        tvJoinCode    = findViewById(R.id.tvJoinCode);
        rvMembers     = findViewById(R.id.rvMembers);
        btnCopyCode   = findViewById(R.id.btnCopyCode);
        btnDeleteTeam = findViewById(R.id.btnDeleteTeam);

        // RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // Handlers
        btnCopyCode.setOnClickListener(v -> copyJoinCode());
        btnDeleteTeam.setOnClickListener(v -> confirmDeleteTeam());

        // Kick off loading
        loadTeam();
    }

    private void loadTeam() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference teamsRef =
                db.collection("users").document(currentUid).collection("teams");

        teamsRef.document(teamId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            // 1) Figure out ownerUid (must have been saved on creation/join)
            ownerUid = doc.getString("ownerUid");
            if (ownerUid == null) {
                ownerUid = currentUid; // fallback for legacy data
            }
            isOwner = currentUid.equals(ownerUid);
            btnDeleteTeam.setVisibility(isOwner ? Button.VISIBLE : Button.GONE);

            // 2) Populate the UI
            tvTeamName.setText(doc.getString("teamName"));
            tvGame.setText("Game: " + doc.getString("game"));
            tvRank.setText("Rank: " + doc.getString("rank"));
            tvJoinCode.setText("Code: " + doc.getString("joinCode"));

            String logoUrl = doc.getString("logoUrl");
            if (logoUrl != null) {
                Glide.with(this).load(logoUrl).into(ivTeamLogo);
            }

            // 3) Load *actual* members from the original team path
            db.collection("users").document(ownerUid)
                    .collection("teams").document(teamId)
                    .collection("members")
                    .get().addOnSuccessListener(qs -> {
                        List<Member> members = new ArrayList<>();
                        for (DocumentSnapshot mDoc : qs.getDocuments()) {
                            members.add(new Member(
                                    mDoc.getId(),
                                    mDoc.getString("name"),
                                    mDoc.getString("role")
                            ));
                        }
                        membersAdapter = new MembersAdapter(members, isOwner);
                        rvMembers.setAdapter(membersAdapter);
                    });
        });
    }

    private void copyJoinCode() {
        String code = tvJoinCode.getText().toString().replace("Code: ", "");
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Team Code", code));
            Toast.makeText(this, "Join code copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteTeam() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Team")
                .setMessage("Are you sure you want to delete this team? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> performTeamDeletion())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performTeamDeletion() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(currentUid)
                .collection("teams").document(teamId)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Team deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
