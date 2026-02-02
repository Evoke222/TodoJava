package com.example.todojava.tasks;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todojava.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText etTaskTitle;
    private Button buttonSaveTask;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);


        View rootView = findViewById(android.R.id.content);

        etTaskTitle = rootView.findViewById(R.id.etTaskTitle);
        buttonSaveTask = rootView.findViewById(R.id.buttonSaveTask);
        ImageButton buttonClose = rootView.findViewById(R.id.buttonClose); // This will now work

        // --- Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // --- Set click listener for the save button
        buttonSaveTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        // --- Set click listener for the new close button
        // This will no longer crash because buttonClose is now found correctly.
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // finish() closes the current activity and goes back to the previous one
                finish();
            }
        });
    }



    private void saveTask() {
        String taskTitle = etTaskTitle.getText().toString().trim();

        // --- Validate that the title is not empty
        if (taskTitle.isEmpty()) {
            etTaskTitle.setError("Title cannot be empty");
            return;
        }

        // --- Validate that the user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Attempted to save task, but user is null.");
            return;
        }

        // --- Disable the button to prevent multiple clicks
        buttonSaveTask.setEnabled(false);
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        // --- Create a new task object (using a Map)
        Map<String, Object> task = new HashMap<>();
        task.put("title", taskTitle);
        task.put("completed", false);
        task.put("owner_uid", currentUser.getUid()); // Link task to the current user
        task.put("created_at", new Date()); // Add a timestamp

        // --- Add a new document with a generated ID to the "tasks" collection
        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        Toast.makeText(AddTaskActivity.this, "Task saved!", Toast.LENGTH_SHORT).show();
                        // --- Close the activity and return to the feed
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        Toast.makeText(AddTaskActivity.this, "Error saving task", Toast.LENGTH_SHORT).show();
                        // --- Re-enable the button on failure
                        buttonSaveTask.setEnabled(true);
                    }
                });
    }
}
