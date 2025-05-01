package com.example.challengermobile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

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
    private ListenerRegistration membersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_dashboard);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Team Details");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Firestore + intent extras
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
        btnLeaveTeam  = findViewById(R.id.btnLeaveTeam);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // Button handlers
        btnCopyCode.setOnClickListener(v -> copyJoinCode());
        btnDeleteTeam.setOnClickListener(v -> confirmDeleteTeam());
        btnLeaveTeam.setOnClickListener(v -> confirmLeaveTeam());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachTeamListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membersListener != null) membersListener.remove();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Up button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attachTeamListener() {
        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference myTeams = db.collection("users")
                .document(me)
                .collection("teams");

        // 1) load my team doc to get ownerUid and metadata
        myTeams.document(teamId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                // team was deleted or I was removed
                navigateToDashboard();
                return;
            }

            ownerUid = doc.getString("ownerUid");
            if (ownerUid == null) ownerUid = me;
            isOwner  = ownerUid.equals(me);

            // show/hide buttons
            btnDeleteTeam.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            btnLeaveTeam .setVisibility(isOwner ? View.GONE   : View.VISIBLE);

            // populate header UI
            tvTeamName.setText(doc.getString("teamName"));
            tvGame.setText("Game: " + doc.getString("game"));
            tvRank.setText("Rank: " + doc.getString("rank"));
            tvJoinCode.setText("Code: " + doc.getString("joinCode"));
            String logoUrl = doc.getString("logoUrl");
            if (logoUrl != null) Glide.with(this).load(logoUrl).into(ivTeamLogo);

            // 2) Listen in real-time to the members subcollection
            if (membersListener != null) membersListener.remove();
            membersListener = db.collection("users")
                    .document(ownerUid)
                    .collection("teams")
                    .document(teamId)
                    .collection("members")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snaps, @Nullable FirebaseFirestoreException e) {
                            if (e != null || snaps == null) return;

                            // Build fresh list, ensuring owner is always present
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
                                // owner not in subcollection yet—add them at top
                                String display = FirebaseAuth.getInstance()
                                        .getCurrentUser().getDisplayName();
                                if (display == null || display.isEmpty()) display = ownerUid;
                                list.add(0, new Member(ownerUid, display, "Owner"));
                            }

                            // bind to adapter
                            if (membersAdapter == null) {
                                membersAdapter = new MembersAdapter(
                                        list,
                                        isOwner,
                                        (memberId, newRole) -> updateMemberRole(memberId, newRole)
                                );
                                rvMembers.setAdapter(membersAdapter);
                            } else {
                                membersAdapter.updateList(list);
                            }
                        }
                    });
        });
    }

    private void updateMemberRole(String memberId, String role) {
        db.collection("users")
                .document(ownerUid)
                .collection("teams")
                .document(teamId)
                .collection("members")
                .document(memberId)
                .update("role", role);
    }

    private void copyJoinCode() {
        String code = tvJoinCode.getText().toString().replace("Code: ", "");
        ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Team Code", code));
            Toast.makeText(this, "Join code copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteTeam() {
        // if more than just the owner, prompt Transfer vs Permanent Delete
        int memberCount = membersAdapter == null ? 0 : membersAdapter.getItemCount();
        if (memberCount > 1) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Team")
                    .setItems(new CharSequence[]{"Transfer Ownership", "Delete Permanently"},
                            (dialog, which) -> {
                                if (which == 0) showTransferDialog();
                                else performPermanentDelete();
                            })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // no one else—just delete
            performPermanentDelete();
        }
    }

    private void showTransferDialog() {
        // build a list of everyone except the owner
        List<Member> all = membersAdapter.getMemberList();
        List<String> names = new ArrayList<>();
        final List<String> uids = new ArrayList<>();
        for (Member m : all) {
            if (!m.getId().equals(ownerUid)) {
                names.add(m.getName());
                uids.add(m.getId());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select New Owner")
                .setSingleChoiceItems(names.toArray(new String[0]), -1,
                        (dlg, idx) -> {
                            String chosenUid = uids.get(idx);
                            transferOwnershipTo(chosenUid);
                            dlg.dismiss();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void transferOwnershipTo(String newOwnerUid) {
        // copy-and-promote logic (omitted here for brevity—you already have it)
        // … migrate the team doc + members subcollection …
        // then:
        Toast.makeText(this, "Ownership transferred", Toast.LENGTH_SHORT).show();
        navigateToDashboard();
    }

    private void performPermanentDelete() {
        // 1) delete every member’s team copy
        db.collection("users")
                .document(ownerUid)
                .collection("teams")
                .document(teamId)
                .collection("members")
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot m : qs) {
                        db.collection("users")
                                .document(m.getId())
                                .collection("teams")
                                .document(teamId)
                                .delete();
                    }
                    // 2) delete owner’s own team doc
                    db.collection("users")
                            .document(ownerUid)
                            .collection("teams")
                            .document(teamId)
                            .delete()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Team deleted", Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            });
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
        // 1) remove my teams/{teamId}
        db.collection("users")
                .document(me)
                .collection("teams")
                .document(teamId)
                .delete()
                .addOnSuccessListener(a -> {
                    // 2) remove me from owner’s members subcollection
                    db.collection("users")
                            .document(ownerUid)
                            .collection("teams")
                            .document(teamId)
                            .collection("members")
                            .document(me)
                            .delete()
                            .addOnSuccessListener(a2 -> {
                                Toast.makeText(this, "You left the team", Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            });
                });
    }

    private void navigateToDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
