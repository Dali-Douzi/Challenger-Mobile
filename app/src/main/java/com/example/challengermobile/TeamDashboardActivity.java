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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_ID = "team_id";

    private FirebaseFirestore db;
    private String teamId, ownerUid;
    private boolean isOwner;

    private TextView tvTeamName, tvGame, tvRank, tvJoinCode;
    private ImageView ivTeamLogo;
    private RecyclerView rvMembers;
    private Button btnCopyCode, btnDeleteTeam, btnLeaveTeam;

    private MembersAdapter membersAdapter;
    private ListenerRegistration myTeamListener;
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

        // View bindings
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

        btnCopyCode.setOnClickListener(v -> copyJoinCode());
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
        if (myTeamListener  != null) myTeamListener.remove();
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
        DocumentReference myTeamRef = db
                .collection("users").document(me)
                .collection("teams").document(teamId);

        myTeamListener = myTeamRef.addSnapshotListener((docSnap, e) -> {
            if (e != null) return;
            if (docSnap == null || !docSnap.exists()) {
                // Team was deleted or user left
                navigateToDashboard();
                return;
            }

            ownerUid = docSnap.getString("ownerUid");
            if (ownerUid == null) ownerUid = me;
            isOwner  = ownerUid.equals(me);

            btnDeleteTeam.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            btnLeaveTeam .setVisibility(isOwner ? View.GONE   : View.VISIBLE);

            // Header UI
            tvTeamName.setText(docSnap.getString("teamName"));
            tvGame.setText("Game: " + docSnap.getString("game"));
            tvRank.setText("Rank: " + docSnap.getString("rank"));
            tvJoinCode.setText("Code: " + docSnap.getString("joinCode"));
            String logo = docSnap.getString("logoUrl");
            if (logo != null) {
                Glide.with(this).load(logo).into(ivTeamLogo);
            }

            attachMembersListener();
        });
    }

    private void attachMembersListener() {
        if (membersListener != null) membersListener.remove();

        membersListener = db
                .collection("users").document(ownerUid)
                .collection("teams").document(teamId)
                .collection("members")
                .addSnapshotListener((snaps, e) -> {
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
                    // Inject owner if missing
                    if (!sawOwner) {
                        String display = FirebaseAuth.getInstance()
                                .getCurrentUser().getDisplayName();
                        if (display == null || display.isEmpty()) display = ownerUid;
                        list.add(0, new Member(ownerUid, display, "Owner"));
                    }

                    if (membersAdapter == null) {
                        membersAdapter = new MembersAdapter(
                                list,
                                isOwner,
                                this::updateMemberRole,
                                this::kickMember
                        );
                        rvMembers.setAdapter(membersAdapter);
                    } else {
                        membersAdapter.updateList(list);
                    }
                });
    }

    private void updateMemberRole(String memberId, String newRole) {
        db.collection("users").document(ownerUid)
                .collection("teams").document(teamId)
                .collection("members").document(memberId)
                .update("role", newRole)
                .addOnSuccessListener(a ->
                        Toast.makeText(this, "Role updated", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void kickMember(String memberId) {
        // Remove from members subcollection
        db.collection("users").document(ownerUid)
                .collection("teams").document(teamId)
                .collection("members").document(memberId)
                .delete()
                .addOnSuccessListener(a -> {
                    // Remove team's reference from that user
                    db.collection("users").document(memberId)
                            .collection("teams").document(teamId)
                            .delete();
                    Toast.makeText(this, "Member kicked", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to kick: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
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
                .setMessage("Are you sure you want to delete this team permanently? This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> performPermanentDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performPermanentDelete() {
        List<Member> members = membersAdapter.getMemberList();
        WriteBatch batch = db.batch();
        for (Member m : members) {
            batch.delete(db.collection("users")
                    .document(m.getId())
                    .collection("teams")
                    .document(teamId));
        }
        batch.delete(db.collection("users")
                .document(ownerUid)
                .collection("teams")
                .document(teamId));

        batch.commit()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Team deleted", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
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
        db.collection("users").document(me)
                .collection("teams").document(teamId)
                .delete()
                .addOnSuccessListener(a -> {
                    db.collection("users").document(ownerUid)
                            .collection("teams").document(teamId)
                            .collection("members").document(me)
                            .delete()
                            .addOnSuccessListener(a2 -> {
                                Toast.makeText(this, "You left the team", Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            })
                            .addOnFailureListener(e2 ->
                                    Toast.makeText(this, "Member remove failed: " + e2.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Leave failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void navigateToDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
