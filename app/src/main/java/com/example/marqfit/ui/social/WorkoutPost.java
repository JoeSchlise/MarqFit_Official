package com.example.marqfit.ui.social;

import com.google.firebase.Timestamp;

public class WorkoutPost {
    private String username;
    private String action;
    private Timestamp timestamp;
    private String profileUrl; // optional

    public WorkoutPost() {}

    public WorkoutPost(String username, String action, Timestamp timestamp, String profileUrl) {
        this.username = username;
        this.action = action;
        this.timestamp = timestamp;
        this.profileUrl = profileUrl;
    }

    public String getUsername() { return username; }
    public String getAction() { return action; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getProfileUrl() { return profileUrl; }
}
