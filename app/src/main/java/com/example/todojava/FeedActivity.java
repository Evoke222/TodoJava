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
import java.util.Comparator;
import java.util.List;

public class FeedActivity extends AppCompatActivity implements OnTaskInteractionListener {

    private static final String TAG = "FeedActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration tasksListener;

    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private ImageButton buttonAddFriend;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
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
        taskAdapter = new TaskAdapter(taskList, this);
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
                loadTasks();
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

        if (tasksListener != null) {
            tasksListener.remove();
        }

        Query query = db.collection("items")
                .whereEqualTo("owner_uid", currentUser.getUid());

        String selectedFilter = filterSpinner.getSelectedItem().toString();

        if (selectedFilter.equals("Tasks")) {
            query = query.whereEqualTo("type", "task");
        } else if (selectedFilter.equals("Events")) {
            query = query.whereEqualTo("type", "event");
        } else {
            query = query.orderBy("created_at", Query.Direction.DESCENDING);
        }

        tasksListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }
            List<Task> newTaskList = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                Task task = doc.toObject(Task.class);
                newTaskList.add(task);
            }

            if (!selectedFilter.equals("All")) {
                Collections.sort(newTaskList, (t1, t2) -> {
                    if (t1.getCreated_at() == null || t2.getCreated_at() == null) return 0;
                    return t2.getCreated_at().compareTo(t1.getCreated_at());
                });
            }

            Log.d(TAG, "Successfully loaded " + newTaskList.size() + " items in real-time.");
            taskAdapter.updateTasks(newTaskList);
        });
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
