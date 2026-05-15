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

    /**
     * שאילתה מספר 9: שליפת משימות פתוחות להיום ושליחת סיכום יומי
     */
    private void fetchAndShowDailySummary(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // קבלת התאריך של היום בפורמט שבו המשימות נשמרות (למשל: 2024-12-25)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        FirebaseFirestore.getInstance().collection("items")
                .whereEqualTo("owner_uid", uid)
                .whereEqualTo("completed", false) // רק משימות שלא בוצעו
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int taskCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        
                        // בדיקה אם התאריך של המשימה הוא היום
                        if (task.getDueDate() != null && task.getDueDate().equals(todayStr)) {
                            taskCount++;
                        }
                    }

                    if (taskCount > 0) {
                        String title = "בוקר טוב!";
                        String message = taskCount == 1 
                            ? "מחכה לך משימה אחת להיום." 
                            : "יש לך " + taskCount + " משימות לביצוע היום.";
                            
                        NotificationHelper.showNotification(context, title, message, 999);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching daily summary", e));
    }
}
