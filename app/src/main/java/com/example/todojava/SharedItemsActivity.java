package com.example.todojava;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todojava.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedItemsActivity extends AppCompatActivity implements SharedItemsAdapter.OnSharedItemInteractionListener {

    private static final String TAG = "SharedItemsActivity";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView sharedItemsRecyclerView;
    private SharedItemsAdapter adapter;
    private List<Task> sharedItemsList = new ArrayList<>();
    private Map<String, String> ownerNames = new HashMap<>();

    private TextView tvNoSharedItems;
    private ImageButton buttonClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_items);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        sharedItemsRecyclerView = findViewById(R.id.sharedItemsRecyclerView);
        tvNoSharedItems = findViewById(R.id.tvNoSharedItems);
        buttonClose = findViewById(R.id.buttonClose);

        setupRecyclerView();

        buttonClose.setOnClickListener(v -> finish());

        if (currentUser != null) {
            loadSharedItems();
        } else {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        adapter = new SharedItemsAdapter(sharedItemsList, ownerNames, this);
        sharedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sharedItemsRecyclerView.setAdapter(adapter);
    }

    private void loadSharedItems() {
        db.collection("items")
                .whereEqualTo("sharedWithUid", currentUser.getUid())
                .whereEqualTo("shareStatus", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sharedItemsList.clear();
                    ownerNames.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoSharedItems.setVisibility(View.VISIBLE);
                        sharedItemsRecyclerView.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    tvNoSharedItems.setVisibility(View.GONE);
                    sharedItemsRecyclerView.setVisibility(View.VISIBLE);

                    List<com.google.android.gms.tasks.Task<DocumentSnapshot>> ownerTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task item = document.toObject(Task.class);
                        item.setDocumentId(document.getId()); // Manually set the document ID
                        sharedItemsList.add(item);
                        ownerTasks.add(db.collection("users").document(item.getOwner_uid()).get());
                    }

                    Tasks.whenAllSuccess(ownerTasks).addOnSuccessListener(objects -> {
                        for (Object object : objects) {
                            DocumentSnapshot ownerDoc = (DocumentSnapshot) object;
                            if (ownerDoc.exists()) {
                                ownerNames.put(ownerDoc.getId(), ownerDoc.getString("username"));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });

                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading shared items", e));
    }

    @Override
    public void onAcceptItem(Task item) {
        db.collection("items").document(item.getDocumentId())
                .update("shareStatus", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item accepted!", Toast.LENGTH_SHORT).show();
                    loadSharedItems(); // Refresh the list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept item.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeclineItem(Task item) {
        db.collection("items").document(item.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item declined.", Toast.LENGTH_SHORT).show();
                    loadSharedItems(); // Refresh the list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to decline item.", Toast.LENGTH_SHORT).show());
    }
}
