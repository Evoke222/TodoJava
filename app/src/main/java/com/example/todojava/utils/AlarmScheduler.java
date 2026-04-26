package com.example.todojava.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.todojava.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    public static void scheduleTaskNotifications(Context context, Task task) {
        if (task == null || task.getDueDate() == null || task.getDocumentId() == null) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dueDate = sdf.parse(task.getDueDate());
            if (dueDate == null) return;

            long dueTimeMillis = dueDate.getTime();

            if ("event".equals(task.getType())) {
                // 1 day before
                scheduleAlarm(context, dueTimeMillis - AlarmManager.INTERVAL_DAY, task.getTitle(), task.getDocumentId(), "ONE_DAY_BEFORE");
                // 30 minutes before (assuming due date is start of day, this might need refinement if task has specific time)
                scheduleAlarm(context, dueTimeMillis - (30 * 60 * 1000), task.getTitle(), task.getDocumentId(), "THIRTY_MIN_BEFORE");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling notification", e);
        }
    }

    private static void scheduleAlarm(Context context, long triggerAtMillis, String title, String docId, String tag) {
        if (triggerAtMillis <= System.currentTimeMillis()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("docId", docId);
        intent.putExtra("tag", tag);

        int requestCode = (docId + tag).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void scheduleDailyMorningReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("type", "DAILY_SUMMARY");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    public static void cancelDailyMorningReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
