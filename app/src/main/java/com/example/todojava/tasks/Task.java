package com.example.todojava.tasks;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Task {
    // @DocumentId tells Firestore to automatically map the document's ID to this field.
    @DocumentId
    private String documentId;

    private String title;
    private boolean completed;
    private String owner_uid;

    // @ServerTimestamp tells Firestore to automatically populate this with the server's time upon creation.
    @ServerTimestamp
    private Date created_at;

    private Date due_date;

    // We will use this later for collaboration.
    private List<String> collaborators;

    // IMPORTANT: Firestore requires a public, empty constructor for deserialization.
    public Task() {}

    // A helpful constructor for when we create new tasks.
    public Task(String title, String owner_uid, Date due_date) {
        this.title = title;
        this.owner_uid = owner_uid;
        this.due_date = due_date;
        this.completed = false; // Tasks always start as not completed.
    }

    // --- Getters and Setters for all fields ---
    // These are required for Firestore to map the data to and from the object.

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getOwner_uid() {
        return owner_uid;
    }

    public void setOwner_uid(String owner_uid) {
        this.owner_uid = owner_uid;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getDue_date() {
        return due_date;
    }

    public void setDue_date(Date due_date) {
        this.due_date = due_date;
    }

    public List<String> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<String> collaborators) {
        this.collaborators = collaborators;
    }

}
