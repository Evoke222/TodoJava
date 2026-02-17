package com.example.todojava.Friends;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todojava.R;
import com.example.todojava.models.FriendRequest;
import com.example.todojava.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FriendsActivity extends AppCompatActivity implements FriendRequestAdapter.OnRequestInteractionListener {

    private static final String TAG = "FriendsActivity";

    // --- UI Elements ---
    private EditText editTextShareId;
    private Button buttonAddFriend;
    private ImageButton buttonClose;
    private RecyclerView requestsRecyclerView;
    private RecyclerView friendsRecyclerView;
    private TextView tvNoRequests;
    private TextView tvNoFriends;

    // --- Adapters and Data Lists ---
    private FriendRequestAdapter requestAdapter;
    private FriendAdapter friendAdapter;
    private List<FriendRequest> requestList;
    private Map<String, User> userMap; // Maps UID to User object to get usernames
    private List<User> friendList;

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // --- Initialize All Views ---
        initializeViews();

        // --- Setup Click Listeners ---
        buttonAddFriend.setOnClickListener(v -> sendFriendRequestFromInput());
        buttonClose.setOnClickListener(v -> finish());

        // --- Setup Both RecyclerViews and Load Data ---
        setupRecyclerViews();
        if (currentUser != null) {
            loadFriendRequests();
            loadFriendsList();
        } else {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        editTextShareId = findViewById(R.id.editTextShareId);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        tvNoRequests = findViewById(R.id.tvNoRequests);
        buttonClose = findViewById(R.id.buttonClose);
        tvNoFriends = findViewById(R.id.tvNoFriends);
    }

    private void setupRecyclerViews() {
        // Setup for Friend Requests List
        requestList = new ArrayList<>();
        userMap = new HashMap<>();
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestAdapter = new FriendRequestAdapter(requestList, userMap, this);
        requestsRecyclerView.setAdapter(requestAdapter);

        // Setup for Current Friends List
        friendList = new ArrayList<>();
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter(friendList);
        friendsRecyclerView.setAdapter(friendAdapter);
    }

    private void loadFriendRequests() {
        if (currentUser == null) return;
        db.collection("friend_requests")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        requestList.clear();
                        userMap.clear();
                        List<String> fromUserIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            FriendRequest request = document.toObject(FriendRequest.class);
                            request.setDocumentId(document.getId());
                            requestList.add(request);
                            if (request.getFromUserId() != null) {
                                fromUserIds.add(request.getFromUserId());
                            }
                        }

                        tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                        requestsRecyclerView.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);

                        if (!fromUserIds.isEmpty()) {
                            // Use the new, corrected method
                            loadUsernamesForRequests(fromUserIds);
                        } else {
                            // If there are no requests, make sure the adapter is notified to clear the view
                            requestAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Error loading friend requests", task.getException());
                    }
                });
    }

    // --- START OF FIX: This method is now completely replaced ---
    private void loadUsernamesForRequests(List<String> userIds) {
        if (userIds.isEmpty()) {
            requestAdapter.notifyDataSetChanged();
            return;
        }
        // Use a counter to know when all async calls are done
        AtomicInteger counter = new AtomicInteger(userIds.size());

        for (String userId : userIds) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // The key is the user's ID, which is the document ID
                                userMap.put(userId, user);
                            }
                        }
                        // Decrement counter and check if this is the last fetch
                        if (counter.decrementAndGet() == 0) {
                            // All user profiles have been fetched, now update the adapter
                            requestAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch user profile for " + userId, e);
                        // Still decrement counter on failure to avoid getting stuck
                        if (counter.decrementAndGet() == 0) {
                            requestAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }
    // --- END OF FIX ---


    private void loadFriendsList() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;

            List<String> friendUids = (List<String>) documentSnapshot.get("friends");

            if (friendUids == null || friendUids.isEmpty()) {
                friendList.clear();
                friendAdapter.notifyDataSetChanged();
                tvNoFriends.setVisibility(View.VISIBLE);
                friendsRecyclerView.setVisibility(View.GONE);
                return;
            }

            // This query for the friends list is also flawed for the same reason.
            // Let's fix it the same way.
            friendList.clear();
            AtomicInteger friendCounter = new AtomicInteger(friendUids.size());

            for (String friendUid : friendUids) {
                db.collection("users").document(friendUid).get().addOnSuccessListener(friendDoc -> {
                    if (friendDoc.exists()) {
                        User user = friendDoc.toObject(User.class);
                        if (user != null) {
                            friendList.add(user);
                        }
                    }
                    if (friendCounter.decrementAndGet() == 0) {
                        friendAdapter.notifyDataSetChanged();
                        tvNoFriends.setVisibility(friendList.isEmpty() ? View.VISIBLE : View.GONE);
                        friendsRecyclerView.setVisibility(friendList.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
            }
        });
    }

    private void sendFriendRequestFromInput() {
        // NOTE: Your 'shareid' field in Firestore is lowercase. We must search for lowercase.
        String shareId = editTextShareId.getText().toString().trim();
        if (shareId.isEmpty()) {
            Toast.makeText(this, "Please enter a Share ID", Toast.LENGTH_SHORT).show();
            return;
        }
        buttonAddFriend.setEnabled(false);
        sendFriendRequest(shareId);
    }

    private void sendFriendRequest(String shareId) {
        final String fromUserId = currentUser.getUid();

        db.collection("users").whereEqualTo("shareid", shareId).limit(1).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().isEmpty()) {
                        Toast.makeText(this, "User not found. Please check the ID.", Toast.LENGTH_LONG).show();
                        buttonAddFriend.setEnabled(true);
                        return;
                    }

                    com.google.firebase.firestore.DocumentSnapshot userDocument = task.getResult().getDocuments().get(0);
                    String toUserId = userDocument.getId();

                    if (fromUserId.equals(toUserId)) {
                        Toast.makeText(this, "You cannot add yourself as a friend.", Toast.LENGTH_SHORT).show();
                        buttonAddFriend.setEnabled(true);
                        return;
                    }

                    if (friendList != null && friendList.stream().anyMatch(user -> toUserId.equals(user.getUid()))) {
                        Toast.makeText(this, "You are already friends with this user.", Toast.LENGTH_SHORT).show();
                        buttonAddFriend.setEnabled(true);
                        return;
                    }

                    // Query 1: Check for an existing request from You -> Them
                    db.collection("friend_requests")
                            .whereEqualTo("fromUserId", fromUserId)
                            .whereEqualTo("toUserId", toUserId)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful() && !task1.getResult().isEmpty()) {
                                    Toast.makeText(this, "You have already sent a request to this user.", Toast.LENGTH_LONG).show();
                                    buttonAddFriend.setEnabled(true);
                                } else {
                                    // Query 2: Check for an existing request from Them -> You
                                    db.collection("friend_requests")
                                            .whereEqualTo("fromUserId", toUserId)
                                            .whereEqualTo("toUserId", fromUserId)
                                            .whereEqualTo("status", "pending")
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                                                    Toast.makeText(this, "This user has already sent you a friend request.", Toast.LENGTH_LONG).show();
                                                    buttonAddFriend.setEnabled(true);
                                                } else {
                                                    createNewRequest(fromUserId, toUserId);
                                                }
                                            });
                                }
                            });
                });
    }

    private void createNewRequest(String fromUserId, String toUserId) {
        FriendRequest request = new FriendRequest(fromUserId, toUserId, "pending");
        db.collection("friend_requests").add(request)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                    editTextShareId.setText("");
                    buttonAddFriend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show();
                    buttonAddFriend.setEnabled(true);
                    Log.e(TAG, "createNewRequest: Failed", e);
                });
    }

    @Override
    public void onAcceptRequest(FriendRequest request) {
        if (request.getDocumentId() == null || request.getDocumentId().isEmpty()) {
            Toast.makeText(this, "Error: Request ID is missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onAcceptRequest: documentId is null!");
            return;
        }

        WriteBatch batch = db.batch();
        batch.update(db.collection("friend_requests").document(request.getDocumentId()), "status", "accepted");
        batch.update(db.collection("users").document(currentUser.getUid()), "friends", FieldValue.arrayUnion(request.getFromUserId()));
        batch.update(db.collection("users").document(request.getFromUserId()), "friends", FieldValue.arrayUnion(currentUser.getUid()));

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Friend added!", Toast.LENGTH_SHORT).show();
            loadFriendRequests();
            loadFriendsList();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to add friend.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onAcceptRequest: Failed", e);
        });
    }

    @Override
    public void onDeclineRequest(FriendRequest request) {
        if (request.getDocumentId() == null || request.getDocumentId().isEmpty()) {
            Toast.makeText(this, "Error: Request ID is missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onDeclineRequest: documentId is null!");
            return;
        }

        db.collection("friend_requests").document(request.getDocumentId())
                .update("status", "declined")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to decline request.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onDeclineRequest: Failed", e);
                });
    }
}
