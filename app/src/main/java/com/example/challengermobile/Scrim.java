package com.example.challengermobile;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

public class Scrim {
    private String id;
    private String teamId;
    private String teamName;
    private String teamRank;            // always held as String internally
    private Date   timestamp;
    private String format;
    private String status;
    private List<String> pendingRequests;

    // Empty constructor for Firestore
    public Scrim() { }

    // Your constructor for new Scrims
    public Scrim(String id,
                 String teamId,
                 String teamName,
                 String teamRank,
                 Date timestamp,
                 String format) {
        this.id             = id;
        this.teamId         = teamId;
        this.teamName       = teamName;
        this.teamRank       = teamRank;
        this.timestamp      = timestamp;
        this.format         = format;
        this.status         = "open";
        this.pendingRequests = null;
    }

    // ─────────────── Getters ───────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String t) { this.teamId = t; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String n) { this.teamName = n; }

    /** Always returns a String, even if the stored Firestore value was numeric. */
    public String getTeamRank() { return teamRank; }

    /** Allow Firestore to call this when deserializing. */
    @PropertyName("teamRank")
    public void setTeamRankFromObject(@Nullable Object rank) {
        if (rank == null) {
            this.teamRank = "";
        } else if (rank instanceof Number) {
            // e.g. 5 → "5"
            this.teamRank = String.valueOf(((Number) rank).intValue());
        } else {
            // assume it's already a String
            this.teamRank = rank.toString();
        }
    }

    @PropertyName("teamRank")
    public @Nullable Object getTeamRankRaw() {
        // tell Firestore where to find the JSON field, but we never use this
        return teamRank;
    }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date d) { this.timestamp = d; }

    public String getFormat() { return format; }
    public void setFormat(String f) { this.format = f; }

    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }

    public List<String> getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(List<String> l) { this.pendingRequests = l; }
}
