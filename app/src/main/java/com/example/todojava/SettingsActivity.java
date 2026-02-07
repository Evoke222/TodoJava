package com.example.todojava;import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Elements
    private ImageButton buttonBack;
    private TextView tvShareId;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Find views by their ID
        buttonBack = findViewById(R.id.buttonBack);
        tvShareId = findViewById(R.id.tvShareId);

        // Set the listener for the back button
        buttonBack.setOnClickListener(v -> finish()); // Closes the activity

        // Fetch and display the user's share ID
        loadUserShareId();
    }

    private void loadUserShareId() {
        if (currentUser == null) {
            Log.w(TAG, "loadUserShareId: No user is logged in.");
            tvShareId.setText("N/A");
            return;
        }

        // Fetch the user's document from the "users" collection
        db.collection("users").document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Get the "shareId" field from the document
                            String shareId = document.getString("shareId");

                            if (shareId != null && !shareId.isEmpty()) {
                                tvShareId.setText(shareId);
                                Log.d(TAG, "Successfully loaded Share ID: " + shareId);
                            } else {
                                // This might happen for users who registered before you added this feature
                                tvShareId.setText("N/A");
                                Log.w(TAG, "Share ID field is missing or empty for user: " + currentUser.getUid());
                            }
                        } else {
                            Log.w(TAG, "User document does not exist for UID: " + currentUser.getUid());
                            tvShareId.setText("N/A");
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user document", task.getException());
                        Toast.makeText(SettingsActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
