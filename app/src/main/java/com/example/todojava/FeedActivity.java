package com.example.todojava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todojava.tasks.AddTaskActivity;
import com.example.todojava.tasks.OnTaskInteractionListener; // <-- ADD THIS IMPORT
import com.example.todojava.tasks.Task;
import com.example.todojava.tasks.TaskAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

// --- STEP 1: IMPLEMENT THE NEW INTERFACE ---
public class FeedActivity extends AppCompatActivity implements OnTaskInteractionListener {

    private static final String TAG = "FeedActivity";

    // Firebase services
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration tasksListener;

    // Views from the layout
    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private Button buttonLogout;

    // Views and Adapter for the Task List
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private FloatingActionButton fabAddTask;

    private ImageButton buttonSettings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        buttonLogout = findViewById(R.id.buttonLogout);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        buttonSettings = findViewById(R.id.buttonSettings);

        // Setup the RecyclerView
        setupRecyclerView();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        fabAddTask.setOnClickListener(view -> {
            Intent intent = new Intent(FeedActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(FeedActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            goToLogin();
        });

        buttonSettings.setOnClickListener(v -> {
            // Create an Intent to open SettingsActivity
            Intent intent = new Intent(FeedActivity.this, SettingsActivity.class);
            startActivity(intent);
        });


        loadUserProfile();
        loadTasks();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tasksListener != null) {
            tasksListener.remove();
            Log.d(TAG, "Tasks listener removed.");
        }
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        // --- STEP 2: MODIFY THIS LINE ---
        // Pass 'this' as the listener because FeedActivity now implements OnTaskInteractionListener
        taskAdapter = new TaskAdapter(taskList, this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
    }

    private void loadUserProfile() {
        // This method remains unchanged
        db.collection("users").document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            tvUsername.setText(document.getString("username"));
                            String pfpUrl = document.getString("pfp_url");
                            if (pfpUrl != null && !pfpUrl.isEmpty()) {
                                Glide.with(this).load(pfpUrl).placeholder(R.drawable.ic_default_profile).error(R.drawable.ic_profile_error).circleCrop().into(ivProfilePicture);
                            }
                        } else {
                            Log.w(TAG, "User document not found for UID: " + currentUser.getUid());
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user document.", task.getException());
                    }
                });
    }

    private void loadTasks() {
        // This method remains unchanged
        if (currentUser == null) {
            Log.w(TAG, "Cannot load tasks, user is not logged in.");
            return;
        }
        tasksListener = db.collection("tasks")
                .whereEqualTo("owner_uid", currentUser.getUid())
                .orderBy("created_at", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    List<Task> newTaskList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Task task = doc.toObject(Task.class);
                        newTaskList.add(task);
                    }
                    Log.d(TAG, "Successfully loaded " + newTaskList.size() + " tasks in real-time.");
                    taskAdapter.updateTasks(newTaskList);
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // --- STEP 3: ADD THE INTERFACE METHOD IMPLEMENTATION ---
    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (currentUser == null || task.getDocumentId() == null) {
            Log.w(TAG, "User not logged in or Task ID is null, cannot update task.");
            return;
        }

        Log.d(TAG, "Updating task: " + task.getDocumentId() + " to completed=" + isChecked);

        // Get the reference to the document in Firestore and update the 'completed' field
        db.collection("tasks").document(task.getDocumentId())
                .update("completed", isChecked)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task 'completed' field successfully updated!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task", e));
    }

    // --- ADD THIS MISSING METHOD TO FIX THE ERROR ---
    @Override
    public void onTaskDeleted(Task task) {
        // First, check if the user is logged in and the task has a valid ID
        if (currentUser == null || task.getDocumentId() == null) {
            Log.w(TAG, "User not logged in or Task ID is null, cannot delete task.");
            return;
        }

        Log.d(TAG, "Deleting task: " + task.getDocumentId());

        // Get the reference to the document in Firestore and delete it
        db.collection("tasks").document(task.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task successfully deleted!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting task", e));

        // You don't need to manually remove the task from the list here.
        // Your real-time listener in loadTasks() will automatically see the change
        // in Firestore and send an updated list to the adapter, which will refresh the UI.
    }
}
