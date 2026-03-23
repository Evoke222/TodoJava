package com.example.todojava;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todojava.Friends.FriendsActivity;
import com.example.todojava.tasks.AddTaskActivity;
import com.example.todojava.tasks.OnTaskInteractionListener;
import com.example.todojava.tasks.Task;
import com.example.todojava.tasks.TaskAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FeedActivity extends AppCompatActivity implements OnTaskInteractionListener {

    private static final String TAG = "FeedActivity";
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "is_dark_mode";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ListenerRegistration ownedItemsListener;
    private ListenerRegistration sharedItemsListener;
    private ListenerRegistration userProfileListener;

    private ImageView ivProfilePicture;
    private TextView tvUsername;
    private ImageButton buttonAddFriend;
    private ImageButton buttonSharedItems;
    private ImageButton buttonCalendar;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;

    private final Map<String, Task> ownedTaskMap = new HashMap<>();
    private final Map<String, Task> sharedTaskMap = new HashMap<>();

    private FloatingActionButton fabAddTask;
    private ImageButton buttonSettings;
    private Spinner filterSpinner;

    private BarChart taskBarChart;
    private Spinner chartTypeSpinner;
    private Button btnCustomRange;
    private TextView tvChartRange;

    private long startDateMs = -1;
    private long endDateMs = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        buttonSettings = findViewById(R.id.buttonSettings);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);
        buttonSharedItems = findViewById(R.id.buttonSharedItems);
        buttonCalendar = findViewById(R.id.buttonCalendar);
        filterSpinner = findViewById(R.id.filterSpinner);
        taskBarChart = findViewById(R.id.taskBarChart);
        chartTypeSpinner = findViewById(R.id.chartTypeSpinner);
        btnCustomRange = findViewById(R.id.btnCustomRange);
        tvChartRange = findViewById(R.id.tvChartRange);

        setupRecyclerView();
        setupFilterSpinner();
        setupChart();
        setupChartControls();
        checkNotificationPermission();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        fabAddTask.setOnClickListener(view -> {
            Intent intent = new Intent(FeedActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        buttonAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        buttonSharedItems.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SharedItemsActivity.class);
            startActivity(intent);
        });

        buttonCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        btnCustomRange.setOnClickListener(v -> showDateRangePicker());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUser != null) {
            startRealTimeListeners();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRealTimeListeners();
    }

    private void startRealTimeListeners() {
        // Remove existing if any
        stopRealTimeListeners();

        // User Profile Listener
        userProfileListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((document, e) -> {
                    if (e != null) return;
                    if (document != null && document.exists()) {
                        tvUsername.setText(document.getString("username"));
                        String pfpUrl = document.getString("pfp_url");
                        if (pfpUrl != null && !pfpUrl.isEmpty()) {
                            Glide.with(this).load(pfpUrl).placeholder(R.drawable.ic_default_profile).circleCrop().into(ivProfilePicture);
                        }
                    }
                });

        // Owned Tasks Listener
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
                    updateUI();
                });

        // Shared Tasks Listener
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
                    updateUI();
                });
    }

    private void stopRealTimeListeners() {
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
        if (ownedItemsListener != null) {
            ownedItemsListener.remove();
            ownedItemsListener = null;
        }
        if (sharedItemsListener != null) {
            sharedItemsListener.remove();
            sharedItemsListener = null;
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupChartControls() {
        String[] options = {"All Items", "Tasks Only", "Events Only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartTypeSpinner.setAdapter(adapter);

        chartTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            startDateMs = selection.first;
            endDateMs = selection.second;
            updateUI();
        });

        picker.show(getSupportFragmentManager(), "RANGE_PICKER");
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupChart() {
        taskBarChart.getDescription().setEnabled(false);
        taskBarChart.setDrawGridBackground(false);
        taskBarChart.setDrawBarShadow(false);
        taskBarChart.getLegend().setEnabled(false);

        XAxis xAxis = taskBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = taskBarChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        taskBarChart.getAxisRight().setEnabled(false);
    }

    private void updateUI() {
        Map<String, Task> combinedMap = new HashMap<>(ownedTaskMap);
        combinedMap.putAll(sharedTaskMap);
        List<Task> allTasks = new ArrayList<>(combinedMap.values());

        updateChartData(allTasks);

        String selectedFilter = filterSpinner.getSelectedItem().toString();
        List<Task> filteredList = new ArrayList<>();

        if (selectedFilter.equals("All")) {
            filteredList.addAll(allTasks);
        } else {
            String filterType = selectedFilter.equals("Tasks") ? "task" : "event";
            for (Task task : allTasks) {
                if (filterType.equals(task.getType())) filteredList.add(task);
            }
        }

        Collections.sort(filteredList, (t1, t2) -> {
            if (t1.getCreated_at() == null || t2.getCreated_at() == null) return 0;
            return t2.getCreated_at().compareTo(t1.getCreated_at());
        });

        taskAdapter.updateTasks(filteredList);
    }

    private void updateChartData(List<Task> allTasks) {
        String filterType = chartTypeSpinner.getSelectedItem().toString();
        Map<String, Integer> dailyCompletions = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        
        List<String> dateLabels = new ArrayList<>();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        if (startDateMs != -1 && endDateMs != -1) {
            cal.setTimeInMillis(startDateMs);
            while (cal.getTimeInMillis() <= endDateMs) {
                String day = sdf.format(cal.getTime());
                dateLabels.add(day);
                dailyCompletions.put(day, 0);
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            tvChartRange.setText("Progress from " + sdf.format(new Date(startDateMs)) + " to " + sdf.format(new Date(endDateMs)));
        } else {
            for (int i = 6; i >= 0; i--) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -i);
                String day = sdf.format(cal.getTime());
                dateLabels.add(day);
                dailyCompletions.put(day, 0);
            }
            tvChartRange.setText("Tasks completed in the last 7 days");
        }

        for (Task task : allTasks) {
            if (task.isCompleted() && task.getCreated_at() != null) {
                if (filterType.equals("Tasks Only") && !task.getType().equals("task")) continue;
                if (filterType.equals("Events Only") && !task.getType().equals("event")) continue;

                String taskDay = sdf.format(task.getCreated_at());
                if (dailyCompletions.containsKey(taskDay)) {
                    dailyCompletions.put(taskDay, dailyCompletions.get(taskDay) + 1);
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < dateLabels.size(); i++) {
            entries.add(new BarEntry(i, dailyCompletions.get(dateLabels.get(i))));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Completed");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        taskBarChart.setData(new BarData(dataSet));
        taskBarChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < dateLabels.size()) ? dateLabels.get(index) : "";
            }
        });
        taskBarChart.invalidate();
    }

    private void goToLogin() {
        Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (currentUser == null || task.getDocumentId() == null) return;
        db.collection("items").document(task.getDocumentId()).update("completed", isChecked);
    }

    @Override
    public void onTaskDeleted(Task task) {
        if (currentUser == null || task.getDocumentId() == null) return;
        db.collection("items").document(task.getDocumentId()).delete();
    }
}
