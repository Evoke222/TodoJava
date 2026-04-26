package com.example.todojava;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todojava.Friends.FriendsActivity;
import com.example.todojava.ai.AiAction;
import com.example.todojava.ai.GeminiService;
import com.example.todojava.tasks.AddTaskActivity;
import com.example.todojava.tasks.OnTaskInteractionListener;
import com.example.todojava.tasks.Task;
import com.example.todojava.tasks.TaskAdapter;
import com.example.todojava.utils.UserImageSelector;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
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

    private ExtendedFloatingActionButton fabAddTask;
    private FloatingActionButton fabAiAssistant;
    private ImageButton buttonSettings;
    private Spinner filterSpinner;

    private BarChart taskBarChart;
    private Spinner chartTypeSpinner;
    private Button btnCustomRange;
    private TextView tvChartRange;

    private ChipGroup dayChipGroup;

    private long startDateMs = -1;
    private long endDateMs = -1;

    private UserImageSelector userImageSelector;
    private GeminiService geminiService;

    private List<AiAction> pendingAiActions = new ArrayList<>();

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
        geminiService = new GeminiService();

        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvUsername = findViewById(R.id.tvUsername);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        fabAiAssistant = findViewById(R.id.fabAiAssistant);
        buttonSettings = findViewById(R.id.buttonSettings);
        buttonAddFriend = findViewById(R.id.buttonAddFriend);
        buttonSharedItems = findViewById(R.id.buttonSharedItems);
        buttonCalendar = findViewById(R.id.buttonCalendar);
        filterSpinner = findViewById(R.id.filterSpinner);
        taskBarChart = findViewById(R.id.taskBarChart);
        chartTypeSpinner = findViewById(R.id.chartTypeSpinner);
        btnCustomRange = findViewById(R.id.btnCustomRange);
        tvChartRange = findViewById(R.id.tvChartRange);
        dayChipGroup = findViewById(R.id.dayChipGroup);

        userImageSelector = new UserImageSelector(this, null);

        setupRecyclerView();
        setupFilterSpinner();
        setupChart();
        setupChartControls();
        checkNotificationPermission();
        setupDaySelector();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        ivProfilePicture.setOnClickListener(v -> showProfileDialog());

        fabAddTask.setOnClickListener(view -> {
            Intent intent = new Intent(FeedActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        fabAiAssistant.setOnClickListener(view -> showAiAssistantDialog());

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

    private void setupDaySelector() {
        dayChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateUI());
    }

    private void showAiAssistantDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_ai_chat, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvAiResponse = sheetView.findViewById(R.id.tvAiResponse);
        EditText etAiInstruction = sheetView.findViewById(R.id.etAiInstruction);
        Button btnAskAi = sheetView.findViewById(R.id.btnAskAi);
        Button btnApplyChanges = sheetView.findViewById(R.id.btnApplyChanges);

        pendingAiActions.clear();

        btnAskAi.setOnClickListener(v -> {
            String instruction = etAiInstruction.getText().toString().trim();
            if (instruction.isEmpty()) return;

            tvAiResponse.setText("Thinking...");
            etAiInstruction.setText("");
            btnApplyChanges.setVisibility(View.GONE);

            Map<String, Task> combinedMap = new HashMap<>(ownedTaskMap);
            combinedMap.putAll(sharedTaskMap);
            List<Task> allTasks = new ArrayList<>(combinedMap.values());

            ListenableFuture<com.google.ai.client.generativeai.type.GenerateContentResponse> future = 
                geminiService.getPlan(allTasks, instruction);

            Futures.addCallback(future, new FutureCallback<com.google.ai.client.generativeai.type.GenerateContentResponse>() {
                @Override
                public void onSuccess(com.google.ai.client.generativeai.type.GenerateContentResponse result) {
                    runOnUiThread(() -> parseAiResponse(result.getText(), tvAiResponse, btnApplyChanges));
                }

                @Override
                public void onFailure(Throwable t) {
                    runOnUiThread(() -> tvAiResponse.setText("Error: " + t.getMessage()));
                }
            }, ContextCompat.getMainExecutor(this));
        });

        btnApplyChanges.setOnClickListener(v -> {
            applyAiActions();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "AI Changes Applied!", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void parseAiResponse(String json, TextView tvResponse, Button btnApply) {
        try {
            Gson gson = new Gson();
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            
            String message = obj.get("message").getAsString();
            tvResponse.setText(message);

            JsonArray actions = obj.getAsJsonArray("actions");
            if (actions != null && actions.size() > 0) {
                pendingAiActions.clear();
                for (int i = 0; i < actions.size(); i++) {
                    JsonObject actionObj = actions.get(i).getAsJsonObject();
                    String typeStr = actionObj.get("type").getAsString();
                    AiAction.Type type = AiAction.Type.valueOf(typeStr);
                    
                    Task task = null;
                    if (actionObj.has("task")) {
                        task = gson.fromJson(actionObj.get("task"), Task.class);
                    }
                    
                    String id = actionObj.has("id") ? actionObj.get("id").getAsString() : null;
                    pendingAiActions.add(new AiAction(type, task, id));
                }
                btnApply.setVisibility(View.VISIBLE);
                btnApply.setText("Apply " + pendingAiActions.size() + " Changes");
            }
        } catch (Exception e) {
            tvResponse.setText("AI: " + json);
            Log.e(TAG, "Parsing error", e);
        }
    }

    private void applyAiActions() {
        for (AiAction action : pendingAiActions) {
            switch (action.getType()) {
                case CREATE:
                    action.getTask().setOwner_uid(currentUser.getUid());
                    db.collection("items").add(action.getTask());
                    break;
                case UPDATE:
                    if (action.getOriginalTaskId() != null) {
                        db.collection("items").document(action.getOriginalTaskId()).set(action.getTask());
                    }
                    break;
                case DELETE:
                    if (action.getOriginalTaskId() != null) {
                        db.collection("items").document(action.getOriginalTaskId()).delete();
                    }
                    break;
            }
        }
    }

    private void showProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_card, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        ImageView dialogIvProfile = dialogView.findViewById(R.id.dialogIvProfile);
        ImageButton btnEditPfp = dialogView.findViewById(R.id.btnEditPfp);
        TextView dialogTvUsername = dialogView.findViewById(R.id.dialogTvUsername);
        TextView dialogTvEmail = dialogView.findViewById(R.id.dialogTvEmail);
        TextView tvMemberSince = dialogView.findViewById(R.id.tvMemberSince);
        Button btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        dialogTvUsername.setText(tvUsername.getText());
        dialogTvEmail.setText(currentUser.getEmail());

        long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMemberSince.setText("Member since: " + sdf.format(new Date(creationTimestamp)));

        if (ivProfilePicture.getDrawable() != null) {
            dialogIvProfile.setImageDrawable(ivProfilePicture.getDrawable());
        }

        userImageSelector.setImageView(dialogIvProfile);

        btnEditPfp.setOnClickListener(v -> userImageSelector.showImageSourceDialog());

        btnCloseDialog.setOnClickListener(v -> {
            File newImage = userImageSelector.createImageFile();
            if (newImage != null) {
                uploadNewProfilePicture(newImage);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void uploadNewProfilePicture(File file) {
        // Correct Fix: Use the default instance to ensure it picks up the bucket from google-services.json
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference()
                .child("profile_pictures/" + currentUser.getUid() + ".jpg");

        storageRef.putFile(Uri.fromFile(file)).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                db.collection("users").document(currentUser.getUid())
                        .update("pfp_url", uri.toString())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed", e);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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
        stopRealTimeListeners();

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
        if (userProfileListener != null) { userProfileListener.remove(); userProfileListener = null; }
        if (ownedItemsListener != null) { ownedItemsListener.remove(); ownedItemsListener = null; }
        if (sharedItemsListener != null) { sharedItemsListener.remove(); sharedItemsListener = null; }
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

        // Apply Type Filter (Task/Event)
        if (selectedFilter.equals("All")) {
            filteredList.addAll(allTasks);
        } else {
            String filterType = selectedFilter.equals("Tasks") ? "task" : "event";
            for (Task task : allTasks) {
                if (filterType.equals(task.getType())) filteredList.add(task);
            }
        }

        // Apply Day Filter (Today/Tomorrow/Upcoming)
        int checkedChipId = dayChipGroup.getCheckedChipId();
        if (checkedChipId != R.id.chipAll) {
            List<Task> dayFilteredList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            Calendar cal = Calendar.getInstance();
            String todayStr = sdf.format(cal.getTime());
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String tomorrowStr = sdf.format(cal.getTime());

            for (Task task : filteredList) {
                if (task.getDueDate() == null) continue;
                
                if (checkedChipId == R.id.chipToday && task.getDueDate().equals(todayStr)) {
                    dayFilteredList.add(task);
                } else if (checkedChipId == R.id.chipTomorrow && task.getDueDate().equals(tomorrowStr)) {
                    dayFilteredList.add(task);
                } else if (checkedChipId == R.id.chipUpcoming) {
                    if (!task.getDueDate().equals(todayStr) && !task.getDueDate().equals(tomorrowStr)) {
                        dayFilteredList.add(task);
                    }
                }
            }
            filteredList = dayFilteredList;
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
