package com.example.todojava;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todojava.tasks.OnTaskInteractionListener;
import com.example.todojava.tasks.Task;
import com.example.todojava.tasks.TaskAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity implements OnTaskInteractionListener {

    private static final String TAG = "CalendarActivity";
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "is_dark_mode";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration ownedItemsListener;
    private ListenerRegistration sharedItemsListener;

    private CalendarView calendarView;
    private RecyclerView calendarRecyclerView;
    private TaskAdapter taskAdapter;
    private TextView tvSelectedDate;
    private TextView tvNoTasks;

    private final Map<String, Task> ownedTaskMap = new HashMap<>();
    private final Map<String, Task> sharedTaskMap = new HashMap<>();
    private String selectedDateString;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(KEY_DARK_MODE, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        calendarView = findViewById(R.id.calendarView);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoTasks = findViewById(R.id.tvNoTasks);
        ImageButton buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());

        setupRecyclerView();

        Calendar today = Calendar.getInstance();
        updateSelectedDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateSelectedDate(year, month, dayOfMonth);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            loadTasksRealTime();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ownedItemsListener != null) {
            ownedItemsListener.remove();
            ownedItemsListener = null;
        }
        if (sharedItemsListener != null) {
            sharedItemsListener.remove();
            sharedItemsListener = null;
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        calendarRecyclerView.setAdapter(taskAdapter);
    }

    private void updateSelectedDate(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        selectedDateString = sdf.format(cal.getTime());
        
        SimpleDateFormat displaySdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        tvSelectedDate.setText("Schedule for " + displaySdf.format(cal.getTime()));
        
        refreshList();
    }

    private void loadTasksRealTime() {
        ownedItemsListener = db.collection("items")
                .whereEqualTo("owner_uid", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    ownedTaskMap.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Task task = doc.toObject(Task.class);
                            ownedTaskMap.put(doc.getId(), task);
                        }
                    }
                    refreshList();
                });

        sharedItemsListener = db.collection("items")
                .whereEqualTo("sharedWithUid", currentUser.getUid())
                .whereEqualTo("shareStatus", "accepted")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    sharedTaskMap.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Task task = doc.toObject(Task.class);
                            sharedTaskMap.put(doc.getId(), task);
                        }
                    }
                    refreshList();
                });
    }

    private void refreshList() {
        Map<String, Task> combinedMap = new HashMap<>(ownedTaskMap);
        combinedMap.putAll(sharedTaskMap);
        
        List<Task> dailyTasks = new ArrayList<>();
        for (Task task : combinedMap.values()) {
            if (task.getDueDate() != null && task.getDueDate().startsWith(selectedDateString)) {
                dailyTasks.add(task);
            }
        }

        if (dailyTasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            calendarRecyclerView.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            calendarRecyclerView.setVisibility(View.VISIBLE);
        }

        taskAdapter.updateTasks(dailyTasks);
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (task.getDocumentId() != null) {
            db.collection("items").document(task.getDocumentId()).update("completed", isChecked);
        }
    }

    @Override
    public void onTaskDeleted(Task task) {
        if (task.getDocumentId() != null) {
            db.collection("items").document(task.getDocumentId()).delete();
        }
    }
}
