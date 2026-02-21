package com.example.todojava.models;

import androidx.annotation.NonNull;

public class User {
    private String username;
    private String uid;
    private String shareId;

    // Firestore requires a public no-argument constructor
    public User() {}

    public User(String username, String uid) {
        this.username = username;
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    // This is important for the ArrayAdapter to display the user's name in the Spinner
    @NonNull
    @Override
    public String toString() {
        return this.username != null ? this.username : "";
    }
}
