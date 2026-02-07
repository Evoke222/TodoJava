package com.example.todojava.tasks;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton; // Import for the delete button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import for the confirmation dialog
import androidx.recyclerview.widget.RecyclerView;

import com.example.todojava.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskInteractionListener listener;

    public TaskAdapter(List<Task> taskList, OnTaskInteractionListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each task item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Get the specific task for this row
        Task task = taskList.get(position);
        // Bind the data and listeners to the view holder
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // This method is called from FeedActivity to update the list
    public void updateTasks(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged(); // This tells the adapter to refresh the entire view
    }


    // The ViewHolder class holds the views for a single item
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        CheckBox cbTaskCompleted;
        ImageButton deleteButton; // 1. Add the delete button view

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs MUST match the IDs in your task_item.xml
            tvTaskTitle = itemView.findViewById(R.id.taskTitleTextView);
            cbTaskCompleted = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.button_delete_task); // 2. Find the delete button
        }

        // A helper method to set all the data and listeners for a view
        public void bind(final Task task, final OnTaskInteractionListener listener) {
            tvTaskTitle.setText(task.getTitle());
            cbTaskCompleted.setChecked(task.isCompleted());

            // Apply or remove strikethrough style
            if (task.isCompleted()) {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Listener for the checkbox
            cbTaskCompleted.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskChecked(task, cbTaskCompleted.isChecked());
                }
            });

            // 3. ADD THE DELETE BUTTON CLICK LISTENER
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    // Show a confirmation dialog to prevent accidental deletion
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete Task")
                            .setMessage("Are you sure you want to delete this task?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // User confirmed, so tell the Activity (via the listener) to delete it
                                listener.onTaskDeleted(task);
                            })
                            .setNegativeButton("Cancel", null) // Do nothing on "Cancel"
                            .show();
                }
            });
        }
    }
}
