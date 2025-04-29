package com.example.challengermobile;

import java.util.Date;

public class Scrim {

    private String id;
    private String teamA;
    private String teamB;
    private Date scrimDate;
    private String status;

    public Scrim(String id, String teamA, String teamB, Date scrimDate, String status) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.scrimDate = scrimDate;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamA() {
        return teamA;
    }

    public void setTeamA(String teamA) {
        this.teamA = teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public void setTeamB(String teamB) {
        this.teamB = teamB;
    }

    public Date getScrimDate() {
        return scrimDate;
    }

    public void setScrimDate(Date scrimDate) {
        this.scrimDate = scrimDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
