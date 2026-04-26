package com.example.todojava.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.todojava.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");
        String docId = intent.getStringExtra("docId");

        if ("DAILY_SUMMARY".equals(type)) {
            fetchAndShowDailySummary(context);
        } else {
            NotificationHelper.showNotification(context, "Reminder: " + title, "Time to check your item!", docId != null ? docId.hashCode() : 0);
        }
    }

    private void fetchAndShowDailySummary(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfDay = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endOfDay = cal.getTime();

        FirebaseFirestore.getInstance().collection("items")
                .whereEqualTo("owner_uid", uid)
                .whereEqualTo("completed", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int taskCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        // Simplified check for today's tasks
                        taskCount++;
                    }

                    if (taskCount > 0) {
                        NotificationHelper.showNotification(context, "Good Morning!", "You have " + taskCount + " tasks remaining for today.", 999);
                    }
                });
    }
}
