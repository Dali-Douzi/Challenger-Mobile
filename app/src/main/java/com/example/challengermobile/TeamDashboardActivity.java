package com.example.challengermobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class TeamDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID = "team_id";

    private FirebaseFirestore db;
    private String teamId, ownerUid;
    private boolean isOwner;

    private TextView    tvTeamName, tvGame, tvRank, tvJoinCode;
    private ImageView   ivTeamLogo;
    private RecyclerView rvMembers;
    private Button      btnCopyCode, btnDeleteTeam, btnLeaveTeam;

    private MembersAdapter      membersAdapter;
    private ListenerRegistration teamListener;
    private ListenerRegistration membersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Team Details");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db     = FirebaseFirestore.getInstance();
        teamId = getIntent().getStringExtra(EXTRA_TEAM_ID);

        tvTeamName    = findViewById(R.id.tvTeamName);
        tvGame        = findViewById(R.id.tvGame);
        tvRank        = findViewById(R.id.tvRank);
        ivTeamLogo    = findViewById(R.id.ivTeamLogo);
        tvJoinCode    = findViewById(R.id.tvJoinCode);
        rvMembers     = findViewById(R.id.rvMembers);
        btnCopyCode   = findViewById(R.id.btnCopyCode);
        btnDeleteTeam = findViewById(R.id.btnDeleteTeam);
        btnLeaveTeam  = findViewById(R.id.btnLeaveTeam);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        btnCopyCode  .setOnClickListener(v -> copyJoinCode());
        btnDeleteTeam.setOnClickListener(v -> confirmPermanentDelete());
        btnLeaveTeam .setOnClickListener(v -> confirmLeaveTeam());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachTeamListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (teamListener != null)   teamListener.remove();
        if (membersListener != null) membersListener.remove();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attachTeamListener() {
        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference teamRef = db.collection("teams").document(teamId);

        teamListener = teamRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException e) {
                if (e != null) return;
                if (doc == null || !doc.exists()) {
                    navigateToDashboard();
                    return;
                }

                ownerUid = doc.getString("ownerUid");
                if (ownerUid == null) ownerUid = me;
                isOwner  = ownerUid.equals(me);

                btnDeleteTeam.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                btnLeaveTeam .setVisibility(isOwner ? View.GONE   : View.VISIBLE);

                tvTeamName.setText(doc.getString("teamName"));
                tvGame    .setText("Game: " + doc.getString("game"));
                tvRank    .setText("Rank: " + doc.getString("rank"));
                tvJoinCode.setText("Code: " + doc.getString("joinCode"));
                String logo = doc.getString("logoUrl");
                if (logo != null) {
                    Glide.with(TeamDashboardActivity.this)
                            .load(logo)
                            .into(ivTeamLogo);
                }

                attachMembersListener();
            }
        });
    }

    private void attachMembersListener() {
        if (membersListener != null) membersListener.remove();

        membersListener = db.collection("teams")
                .document(teamId)
                .collection("members")
                .addSnapshotListener(new EventListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable com.google.firebase.firestore.QuerySnapshot snaps,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null || snaps == null) return;

                        List<Member> list = new ArrayList<>();
                        boolean sawOwner = false;
                        for (DocumentSnapshot mDoc : snaps.getDocuments()) {
                            String id   = mDoc.getId();
                            String name = mDoc.getString("name");
                            String role = mDoc.getString("role");
                            list.add(new Member(id, name, role));
                            if (id.equals(ownerUid)) sawOwner = true;
                        }
                        if (!sawOwner) {
                            String display = FirebaseAuth.getInstance()
                                    .getCurrentUser().getDisplayName();
                            if (display == null || display.isEmpty()) display = ownerUid;
                            list.add(0, new Member(ownerUid, display, "Owner"));
                        }

                        if (membersAdapter == null) {
                            membersAdapter = new MembersAdapter(
                                    list, isOwner,
                                    TeamDashboardActivity.this::updateMemberRole,
                                    TeamDashboardActivity.this::kickMember
                            );
                            rvMembers.setAdapter(membersAdapter);
                        } else {
                            membersAdapter.updateList(list);
                        }
                    }
                });
    }

    private void updateMemberRole(String memberId, String newRole) {
        db.collection("teams")
                .document(teamId)
                .collection("members")
                .document(memberId)
                .update("role", newRole)
                .addOnSuccessListener(a ->
                        Toast.makeText(this, "Role updated", Toast.LENGTH_SHORT).show()
                );
    }

    private void kickMember(String memberId) {
        // Remove from global members
        db.collection("teams")
                .document(teamId)
                .collection("members")
                .document(memberId)
                .delete()
                .addOnSuccessListener(a -> {
                    // Clean up back-ref under that user
                    db.collection("users")
                            .document(memberId)
                            .collection("teams")
                            .document(teamId)
                            .delete();
                    Toast.makeText(this, "Member kicked", Toast.LENGTH_SHORT).show();
                });
    }

    private void copyJoinCode() {
        String code = tvJoinCode.getText().toString().replace("Code: ", "");
        ClipboardManager cm = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Team Code", code));
            Toast.makeText(this, "Join code copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmPermanentDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Team")
                .setMessage("Are you sure you want to delete this team permanently? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> performPermanentDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performPermanentDelete() {
        List<Member> members = membersAdapter.getMemberList();
        WriteBatch batch = db.batch();

        // Remove per-user back-refs
        for (Member m : members) {
            batch.delete(db.collection("users")
                    .document(m.getId())
                    .collection("teams")
                    .document(teamId));
        }
        // Remove the global team document
        batch.delete(db.collection("teams").document(teamId));

        batch.commit()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Team deleted", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                });
    }

    private void confirmLeaveTeam() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Team")
                .setMessage("Are you sure you want to leave this team?")
                .setPositiveButton("Leave", (d, w) -> performLeaveTeam())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLeaveTeam() {
        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Remove from global members
        db.collection("teams")
                .document(teamId)
                .collection("members")
                .document(me)
                .delete()
                .addOnSuccessListener(a -> {
                    // Remove back-ref under /users
                    db.collection("users")
                            .document(me)
                            .collection("teams")
                            .document(teamId)
                            .delete();
                    Toast.makeText(this, "You left the team", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                });
    }

    private void navigateToDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
