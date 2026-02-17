package com.example.todojava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button; // Import the Button class
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

    // --- UI Elements ---
    private ImageButton buttonBack;
    private TextView tvShareId;
    private Button buttonLogout; // <<--- 1. DECLARE the logout button

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // <<--- 2. DECLARE FirebaseAuth
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // --- Initialize Firebase instances ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // <<--- 3. INITIALIZE FirebaseAuth
        currentUser = mAuth.getCurrentUser();

        // --- Find views by their ID ---
        buttonBack = findViewById(R.id.buttonBack);
        tvShareId = findViewById(R.id.tvShareId);
        buttonLogout = findViewById(R.id.buttonLogout); // <<--- 4. INITIALIZE the logout button

        // --- Set Listeners ---
        buttonBack.setOnClickListener(v -> finish()); // Closes the activity

        // Set the listener for the logout button
        buttonLogout.setOnClickListener(v -> { // <<--- 5. ADD the click listener
            // Sign the user out
            mAuth.signOut();

            // Create an Intent to go to the LoginActivity
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);

            // Add flags to clear the activity stack. This prevents the user
            // from pressing the back button and returning to the logged-in state.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish the current activity
            finish();
        });


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
