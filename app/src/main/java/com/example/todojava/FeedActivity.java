package com.example.todojava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todojava.Friends.FriendsActivity;
import com.example.todojava.tasks.AddTaskActivity;
import com.example.todojava.tasks.OnTaskInteractionListener;
import com.example.todojava.tasks.Task;
import com.example.todojava.tasks.TaskAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedActivity extends AppCompatActivity implements OnTaskInteractionListener {

    private static final String TAG = "FeedActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Listeners for real-time updates
    private ListenerRegistration ownedItemsListener;
    private ListenerRegistration sharedItemsListener;

    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private ImageButton buttonAddFriend;
    private ImageButton buttonSharedItems;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;

    // Separate lists for owned and shared items
    private List<Task> ownedTaskList = new ArrayList<>();
    private List<Task> sharedTaskList = new ArrayList<>();

    private FloatingActionButton fabAddTask;
    private ImageButton buttonSettings;
    private Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        buttonSettings = findViewById(R.id.buttonSettings);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);
        buttonSharedItems = findViewById(R.id.buttonSharedItems);
        filterSpinner = findViewById(R.id.filterSpinner);

        setupRecyclerView();
        setupFilterSpinner();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        fabAddTask.setOnClickListener(view -> {
            Intent intent = new Intent(FeedActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        buttonAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        buttonSharedItems.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SharedItemsActivity.class);
            startActivity(intent);
        });

        loadUserProfile();
        loadTasks();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listeners to prevent memory leaks
        if (ownedItemsListener != null) {
            ownedItemsListener.remove();
        }
        if (sharedItemsListener != null) {
            sharedItemsListener.remove();
        }
    }

    private void setupRecyclerView() {
        // The adapter is initialized with an empty list.
        // It will be updated by loadTasks().
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When filter changes, re-run the filtering and update the adapter
                updateAndFilterTaskList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUserProfile() {
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
        if (currentUser == null) {
            Log.w(TAG, "Cannot load tasks, user is not logged in.");
            return;
        }

        // Clean up previous listeners if they exist
        if (ownedItemsListener != null) ownedItemsListener.remove();
        if (sharedItemsListener != null) sharedItemsListener.remove();

        // Query 1: Items owned by the current user
        Query ownedQuery = db.collection("items")
                .whereEqualTo("owner_uid", currentUser.getUid());

        ownedItemsListener = ownedQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for owned items.", e);
                return;
            }
            ownedTaskList.clear();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Task task = doc.toObject(Task.class);
                    task.setDocumentId(doc.getId()); // Manually set the document ID
                    ownedTaskList.add(task);
                }
            }
            Log.d(TAG, "Owned items updated: " + ownedTaskList.size());
            updateAndFilterTaskList();
        });

        // Query 2: Items shared with the current user that are "accepted"
        Query sharedQuery = db.collection("items")
                .whereEqualTo("sharedWithUid", currentUser.getUid())
                .whereEqualTo("shareStatus", "accepted");

        sharedItemsListener = sharedQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for shared items.", e);
                return;
            }
            sharedTaskList.clear();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Task task = doc.toObject(Task.class);
                    task.setDocumentId(doc.getId()); // Manually set the document ID
                    sharedTaskList.add(task);
                }
            }
            Log.d(TAG, "Shared items updated: " + sharedTaskList.size());
            updateAndFilterTaskList();
        });
    }

    private void updateAndFilterTaskList() {
        // Use a Map to combine lists and prevent duplicates
        Map<String, Task> taskMap = new HashMap<>();
        for (Task task : ownedTaskList) {
            if (task.getDocumentId() != null) { // Null check to prevent crash
                taskMap.put(task.getDocumentId(), task);
            }
        }
        for (Task task : sharedTaskList) {
            if (task.getDocumentId() != null) { // Null check to prevent crash
                taskMap.put(task.getDocumentId(), task);
            }
        }
        List<Task> combinedList = new ArrayList<>(taskMap.values());

        // Apply filtering based on the spinner selection
        String selectedFilter = filterSpinner.getSelectedItem().toString();
        List<Task> filteredList = new ArrayList<>();

        if (selectedFilter.equals("All")) {
            filteredList.addAll(combinedList);
        } else {
            String filterType = selectedFilter.equals("Tasks") ? "task" : "event";
            for (Task task : combinedList) {
                if (filterType.equals(task.getType())) {
                    filteredList.add(task);
                }
            }
        }

        // Sort the final list by creation date
        Collections.sort(filteredList, (t1, t2) -> {
            if (t1.getCreated_at() == null || t2.getCreated_at() == null) return 0;
            return t2.getCreated_at().compareTo(t1.getCreated_at());
        });

        Log.d(TAG, "Updating adapter with " + filteredList.size() + " items.");
        taskAdapter.updateTasks(filteredList);
    }

    private void goToLogin() {
        Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (currentUser == null || task.getDocumentId() == null) {
            Log.w(TAG, "User not logged in or Task ID is null, cannot update task.");
            return;
        }
        db.collection("items").document(task.getDocumentId())
                .update("completed", isChecked)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task 'completed' field successfully updated!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task", e));
    }

    @Override
    public void onTaskDeleted(Task task) {
        if (currentUser == null || task.getDocumentId() == null) {
            Log.w(TAG, "User not logged in or Task ID is null, cannot delete task.");
            return;
        }
        db.collection("items").document(task.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task successfully deleted!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting task", e));
    }
}
