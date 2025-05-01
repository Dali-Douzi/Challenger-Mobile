package com.example.challengermobile;

public class Member {
    private String id;
    private String name;
    private String role;

    public Member(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getId()   { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }

    public void setName(String name) { this.name = name; }
    public void setRole(String role) { this.role = role; }
}