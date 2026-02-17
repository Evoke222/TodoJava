package com.example.todojava.models;

import com.google.firebase.firestore.Exclude;

public class User {
    private String username;
    private String shareId;

    // 1. ADD THIS UID FIELD
    private String uid;

    // Required empty public constructor for Firestore
    public User() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    // 2. ADD THESE GETTER AND SETTER METHODS
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
}
