package com.example.todojava.tasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.todojava.R;
import com.example.todojava.models.User;
import com.example.todojava.notifications.TaskReminderReceiver;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AddTaskActivity extends AppCompatActivity {

    private static final String TAG = "AddTaskActivity";

    private EditText etTaskTitle;
    private EditText etTaskDueDate;
    private EditText etTaskDetails;
    private CheckBox cbRemindMe;
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
        cbRemindMe = findViewById(R.id.cbRemindMe);
        buttonSaveTask = findViewById(R.id.buttonSaveTask);
        ImageButton buttonClose = findViewById(R.id.buttonClose);
        rgTaskType = findViewById(R.id.rgTaskType);
        spinnerShareWith = findViewById(R.id.spinnerShareWith);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupFriendSpinner();
        loadFriends();

        etTaskDueDate.setOnClickListener(v -> showMaterialDatePicker());

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

        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;
            List<String> friendUids = (List<String>) documentSnapshot.get("friends");
            friendsList.clear();
            User noneUser = new User();
            noneUser.setUsername("None");
            noneUser.setUid("");
            friendsList.add(noneUser);

            if (friendUids == null || friendUids.isEmpty()) {
                friendsAdapter.notifyDataSetChanged();
                return;
            }

            List<com.google.android.gms.tasks.Task<DocumentSnapshot>> friendTasks = new ArrayList<>();
            for (String friendUid : friendUids) friendTasks.add(db.collection("users").document(friendUid).get());

            Tasks.whenAllSuccess(friendTasks).addOnSuccessListener(objects -> {
                for (Object object : objects) {
                    DocumentSnapshot doc = (DocumentSnapshot) object;
                    User friend = doc.toObject(User.class);
                    if (friend != null) {
                        friend.setUid(doc.getId());
                        friendsList.add(friend);
                    }
                }
                friendsAdapter.notifyDataSetChanged();
            });
        });
    }

    private void showMaterialDatePicker() {
        CalendarConstraints constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Use local timezone for the Calendar object to match TimePicker and formatter
            selectedDueDate = Calendar.getInstance();
            selectedDueDate.setTimeInMillis(selection);
            showMaterialTimePicker();
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showMaterialTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedDueDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedDueDate.set(Calendar.MINUTE, timePicker.getMinute());
            
            // Format using local time to match CalendarActivity filtering
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            etTaskDueDate.setText(sdf.format(selectedDueDate.getTime()));
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void saveTask() {
        String taskTitle = etTaskTitle.getText().toString().trim();
        String taskDueDate = etTaskDueDate.getText().toString().trim();
        String taskDetails = etTaskDetails.getText().toString().trim();
        boolean remindMe = cbRemindMe.isChecked();

        if (taskTitle.isEmpty()) {
            etTaskTitle.setError("Title cannot be empty");
            return;
        }

        if (currentUser == null) return;

        buttonSaveTask.setEnabled(false);
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
        item.put("remindMe", remindMe);

        if (selectedFriend != null && selectedFriend.getUid() != null && !selectedFriend.getUid().isEmpty()) {
            item.put("sharedWithUid", selectedFriend.getUid());
            item.put("shareStatus", "pending");
        }

        db.collection("items")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    if (remindMe) {
                        scheduleReminder(taskTitle, taskDetails, taskDueDate);
                        // Confirmation notification when task is created
                        TaskReminderReceiver.showNotification(this, 
                                "Task Reminder Set", 
                                "You will be notified for: " + taskTitle + " at " + taskDueDate);
                    }
                    Toast.makeText(AddTaskActivity.this, "Item saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSaveTask.setEnabled(true);
                });
    }

    private void scheduleReminder(String title, String details, String dueDate) {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notifications_enabled", true)) return;
        if (selectedDueDate == null) return;

        long reminderTime = selectedDueDate.getTimeInMillis() - (5 * 60 * 1000);
        if (reminderTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Reminder time is in the past, skipping.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TaskReminderReceiver.class);
        intent.putExtra("task_title", title);
        intent.putExtra("task_details", details);
        intent.putExtra("task_due_date", dueDate);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(permissionIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }
}
