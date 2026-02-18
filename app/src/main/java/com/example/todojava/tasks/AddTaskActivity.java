package com.example.todojava.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText etTaskTitle;
    private EditText etTaskDueDate;
    private EditText etTaskDetails;
    private Button buttonSaveTask;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Calendar selectedDueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);

        View rootView = findViewById(android.R.id.content);

        etTaskTitle = rootView.findViewById(R.id.etTaskTitle);
        etTaskDueDate = rootView.findViewById(R.id.etTaskDueDate);
        etTaskDetails = rootView.findViewById(R.id.etTaskDetails);
        buttonSaveTask = rootView.findViewById(R.id.buttonSaveTask);
        ImageButton buttonClose = rootView.findViewById(R.id.buttonClose);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        etTaskDueDate.setOnClickListener(v -> showDatePickerDialog());

        buttonSaveTask.setOnClickListener(v -> saveTask());

        buttonClose.setOnClickListener(v -> finish());
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(year1, monthOfYear, dayOfMonth);
                    showTimePickerDialog();
                }, year, month, day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    selectedDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDueDate.set(Calendar.MINUTE, minute1);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                    etTaskDueDate.setText(sdf.format(selectedDueDate.getTime()));
                }, hour, minute, false);
        timePickerDialog.show();
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

        Map<String, Object> task = new HashMap<>();
        task.put("title", taskTitle);
        task.put("dueDate", taskDueDate);
        task.put("details", taskDetails);
        task.put("completed", false);
        task.put("owner_uid", currentUser.getUid());
        task.put("created_at", new Date());

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddTaskActivity.this, "Task saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddTaskActivity.this, "Error saving task", Toast.LENGTH_SHORT).show();
                    buttonSaveTask.setEnabled(true);
                });
    }
}
