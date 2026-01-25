package com.example.todojava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.todojava.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private Button buttonLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        // 1. Initialize Firebase and Views
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        buttonLogout = findViewById(R.id.buttonLogout);

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // 2. Load the user's profile information
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // This case should ideally not happen if the user just logged in/registered,
            // but it's good practice to handle it.
            Log.w(TAG, "loadUserProfile: No user is signed in.");
            // Optional: redirect to login
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading profile for user ID: " + userId);

        // 3. Fetch the user document from Firestore
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d(TAG, "User document found.");

                            // 4. Get username and pfp_url from the document
                            String username = document.getString("username");
                            String pfpUrl = document.getString("pfp_url");

                            // Set the username in the TextView
                            tvUsername.setText(username);

                            // 5. Use Glide to load the profile picture
                            if (pfpUrl != null && !pfpUrl.isEmpty()) {
                                Glide.with(FeedActivity.this)
                                        .load(pfpUrl)
                                        .placeholder(R.drawable.ic_default_profile) // Create a default placeholder drawable
                                        .error(R.drawable.ic_profile_error)     // Create an error placeholder drawable
                                        .circleCrop() // Makes the image circular - highly recommended!
                                        .into(ivProfilePicture);
                            } else {
                                // If pfp_url is empty, load a default image
                                Log.d(TAG, "pfp_url is empty. Loading default avatar.");
                                ivProfilePicture.setImageResource(R.drawable.ic_default_profile);
                            }

                        } else {
                            Log.e(TAG, "User document not found in Firestore for UID: " + userId);
                            Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user document.", task.getException());
                        Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
