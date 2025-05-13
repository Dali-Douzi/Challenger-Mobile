package com.example.challengermobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private FirebaseAuth       mAuth;
    private FirebaseFirestore  db;
    private RecyclerView       notificationsRecyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Notifications");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications);
        notificationsRecyclerView.setAdapter(adapter);

        fetchNotifications();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Back arrow
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchNotifications() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("scrims")
                .whereEqualTo("ownerUid", user.getUid())
                .get()
                .addOnSuccessListener(qs -> {
                    notifications.clear();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String scrimId    = doc.getId();
                        String teamName   = doc.getString("teamName");
                        String teamRank   = doc.get("teamRank") != null
                                ? doc.get("teamRank").toString() : "";
                        @SuppressWarnings("unchecked")
                        List<String> pend = (List<String>) doc.get("pendingRequests");
                        if (pend != null) {
                            for (String requesterUid : pend) {
                                notifications.add(
                                        new NotificationItem(scrimId, teamName, teamRank, requesterUid)
                                );
                            }
                        }
                    }
                    if (notifications.isEmpty()) {
                        Toast.makeText(this, "No notifications", Toast.LENGTH_SHORT).show();
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notifications: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private static class NotificationItem {
        String scrimId, teamName, teamRank, requesterUid;
        NotificationItem(String s, String t, String r, String u) {
            scrimId = s; teamName = t; teamRank = r; requesterUid = u;
        }
    }

    private class NotificationAdapter
            extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        private final List<NotificationItem> items;
        NotificationAdapter(List<NotificationItem> list) { items = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int pos) {
            NotificationItem it = items.get(pos);
            h.title.setText("Request from " + it.teamName);
            h.itemView.setOnClickListener(v -> {
                // show info dialog with confirm/decline
                new AlertDialog.Builder(NotificationsActivity.this)
                        .setTitle("Scrim Request")
                        .setMessage(
                                "Team: " + it.teamName + "\n" +
                                        "Rank: " + it.teamRank
                        )
                        .setPositiveButton("Confirm", (dlg, which) -> {
                            db.collection("scrims")
                                    .document(it.scrimId)
                                    .update(
                                            "status", "booked",
                                            "pendingRequests", FieldValue.arrayRemove(it.requesterUid)
                                    )
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(
                                                NotificationsActivity.this,
                                                "Scrim booked",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        items.remove(pos);
                                        notifyItemRemoved(pos);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    NotificationsActivity.this,
                                                    "Error booking scrim: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        })
                        .setNegativeButton("Decline", (dlg, which) -> {
                            db.collection("scrims")
                                    .document(it.scrimId)
                                    .update(
                                            "pendingRequests", FieldValue.arrayRemove(it.requesterUid)
                                    )
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(
                                                NotificationsActivity.this,
                                                "Request declined",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                        items.remove(pos);
                                        notifyItemRemoved(pos);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    NotificationsActivity.this,
                                                    "Error declining request: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        })
                        .show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(android.R.id.text1);
            }
        }
    }
}
