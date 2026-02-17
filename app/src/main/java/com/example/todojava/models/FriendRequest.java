package com.example.todojava.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class FriendRequest {

    // This field will hold the document's ID from Firestore.
    // @Exclude prevents Firestore from trying to save this field back into the document.
    @Exclude
    private String documentId;

    private String fromUserId;
    private String toUserId;
    private String status;
    @ServerTimestamp
    private Date timestamp;

    // Required empty public constructor
    public FriendRequest() {}

    public FriendRequest(String fromUserId, String toUserId, String status) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = status;
    }

    // --- Getters and Setters ---

    // ⬇️ ADD THESE TWO METHODS ⬇️
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    // ⬆️ END OF NEW METHODS ⬆️

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
