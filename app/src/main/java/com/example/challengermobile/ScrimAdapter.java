package com.example.challengermobile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScrimAdapter extends RecyclerView.Adapter<ScrimAdapter.ScrimViewHolder> {

    private List<Scrim>       scrims;
    private List<String>      userTeamIds;
    private FirebaseFirestore db;

    public ScrimAdapter(List<Scrim> scrims, List<String> userTeamIds) {
        this.scrims       = scrims;
        this.userTeamIds  = userTeamIds;
        this.db           = FirebaseFirestore.getInstance();
    }

    /** Called from DashboardActivity whenever the user’s team-list changes */
    public void setUserTeamIds(List<String> userTeamIds) {
        this.userTeamIds = userTeamIds;
    }

    @NonNull
    @Override
    public ScrimViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scrim_item, parent, false);
        return new ScrimViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ScrimViewHolder h, int pos
    ) {
        Scrim s = scrims.get(pos);

        // Text fields
        h.teamNameText   .setText(s.getTeamName());
        h.teamRankText   .setText("Rank: " + s.getTeamRank());
        String when = new SimpleDateFormat(
                "MMM d, HH:mm", Locale.getDefault()
        ).format(s.getTimestamp());
        h.timeFormatText .setText(when + " • " + s.getFormat());

        // 1) Already BOOKED?
        if ("booked".equals(s.getStatus())) {
            h.requestButton.setText("BOOKED");
            h.requestButton.setEnabled(false);

            // 2) Posted by one of *your* teams → Edit
        } else if (userTeamIds.contains(s.getTeamId())) {
            h.requestButton.setText("Edit");
            h.requestButton.setEnabled(true);
            h.requestButton.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), EditScrimActivity.class);
                i.putExtra(EditScrimActivity.EXTRA_SCRIM_ID, s.getId());
                v.getContext().startActivity(i);
            });

            // 3) Sent already by *one* of your teams → Sent
        } else {
            boolean hasSent = false;
            if (s.getPendingRequests() != null) {
                for (String tid : userTeamIds) {
                    if (s.getPendingRequests().contains(tid)) {
                        hasSent = true;
                        break;
                    }
                }
            }
            if (hasSent) {
                h.requestButton.setText("Sent");
                h.requestButton.setEnabled(false);

                // 4) Otherwise “Send Scrim Request”:
            } else {
                h.requestButton.setText("Send Scrim Request");

                // If you have no teams, disable and inform
                if (userTeamIds.isEmpty()) {
                    h.requestButton.setEnabled(false);
                    h.requestButton.setOnClickListener(v ->
                            Toast.makeText(v.getContext(),
                                    "You must join a team first",
                                    Toast.LENGTH_SHORT).show()
                    );

                    // Otherwise, send using your first teamId
                } else {
                    h.requestButton.setEnabled(true);
                    h.requestButton.setOnClickListener(v -> {
                        String myTeamId = userTeamIds.get(0);
                        db.collection("scrims")
                                .document(s.getId())
                                .update("pendingRequests",
                                        FieldValue.arrayUnion(myTeamId))
                                .addOnSuccessListener(a ->
                                        Toast.makeText(v.getContext(),
                                                "Request Sent",
                                                Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(v.getContext(),
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return scrims.size();
    }

    /** Replace the data & refresh the list */
    public void updateList(List<Scrim> newList) {
        this.scrims = newList;
        notifyDataSetChanged();
    }

    static class ScrimViewHolder extends RecyclerView.ViewHolder {
        TextView teamNameText, teamRankText, timeFormatText;
        Button   requestButton;

        ScrimViewHolder(@NonNull View itemView) {
            super(itemView);
            teamNameText    = itemView.findViewById(R.id.scrimTeamName);
            teamRankText    = itemView.findViewById(R.id.scrimTeamRank);
            timeFormatText  = itemView.findViewById(R.id.scrimTimeFormat);
            requestButton   = itemView.findViewById(R.id.scrimRequestButton);
        }
    }
}
