package com.example.todojava.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.todojava.R;
import com.example.todojava.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText etTaskTitle;
    private EditText etTaskDueDate;
    private EditText etTaskDetails;
    private Button buttonSaveTask;
    private RadioGroup rgTaskType;
    private Spinner spinnerShareWith;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Calendar selectedDueDate;

    private List<User> friendsList = new ArrayList<>();
    private ArrayAdapter<User> friendsAdapter;
    private User selectedFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDueDate = findViewById(R.id.etTaskDueDate);
        etTaskDetails = findViewById(R.id.etTaskDetails);
        buttonSaveTask = findViewById(R.id.buttonSaveTask);
        ImageButton buttonClose = findViewById(R.id.buttonClose);
        rgTaskType = findViewById(R.id.rgTaskType);
        spinnerShareWith = findViewById(R.id.spinnerShareWith);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupFriendSpinner();
        loadFriends();

        etTaskDueDate.setOnClickListener(v -> showDatePickerDialog());

        buttonSaveTask.setOnClickListener(v -> saveTask());

        buttonClose.setOnClickListener(v -> finish());
    }

    private void setupFriendSpinner() {
        friendsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, friendsList);
        friendsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShareWith.setAdapter(friendsAdapter);

        spinnerShareWith.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFriend = (User) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFriend = null;
            }
        });
    }

    private void loadFriends() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("friends")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendsList.clear();

                        User noneUser = new User();
                        noneUser.setUsername("None");
                        noneUser.setUid("");
                        friendsList.add(noneUser);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User friend = document.toObject(User.class);
                            friendsList.add(friend);
                        }
                        friendsAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error getting friends.", task.getException());
                    }
                });
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(year1, monthOfYear, dayOfMonth);
                    showTimePickerDialog();
                }, year, month, day).show();
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDueDate.set(Calendar.MINUTE, minute1);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                    etTaskDueDate.setText(sdf.format(selectedDueDate.getTime()));
                }, hour, minute, false).show();
    }

    private void saveTask() {
        String taskTitle = etTaskTitle.getText().toString().trim();
        String taskDueDate = etTaskDueDate.getText().toString().trim();
        String taskDetails = etTaskDetails.getText().toString().trim();

        if (taskTitle.isEmpty()) {
            etTaskTitle.setError("Title cannot be empty");
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Attempted to save task, but user is null.");
            return;
        }

        buttonSaveTask.setEnabled(false);
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        int selectedId = rgTaskType.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        String type = selectedRadioButton.getText().toString().toLowerCase();

        Map<String, Object> item = new HashMap<>();
        item.put("title", taskTitle);
        item.put("dueDate", taskDueDate);
        item.put("details", taskDetails);
        item.put("completed", false);
        item.put("owner_uid", currentUser.getUid());
        item.put("created_at", new Date());
        item.put("type", type);

        if (selectedFriend != null && !selectedFriend.getUid().isEmpty()) {
            item.put("sharedWithUid", selectedFriend.getUid());
            item.put("shareStatus", "pending");
        }

        db.collection("items")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddTaskActivity.this, "Item saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddTaskActivity.this, "Error saving item", Toast.LENGTH_SHORT).show();
                    buttonSaveTask.setEnabled(true);
                });
    }
}
