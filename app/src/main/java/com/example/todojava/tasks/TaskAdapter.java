package com.example.todojava.tasks;

import android.graphics.Paint; // <-- ADD THIS IMPORT
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox; // <-- ADD THIS IMPORT
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todojava.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskInteractionListener listener; // <-- ADD THIS VARIABLE

    // --- MODIFY THE CONSTRUCTOR to accept the listener ---
    public TaskAdapter(List<Task> taskList, OnTaskInteractionListener listener) {
        this.taskList = taskList;
        this.listener = listener; // <-- INITIALIZE THE LISTENER
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the correct layout file name: task_item.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    // --- MODIFY onBindViewHolder to use a new 'bind' method ---
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener); // <-- We will create this 'bind' method below
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // --- MODIFY THE VIEWHOLDER CLASS SIGNIFICANTLY ---
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        CheckBox cbTaskCompleted; // <-- ADD CHECKBOX VIEW

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Use the correct IDs from your task_item.xml
            tvTaskTitle = itemView.findViewById(R.id.taskTitleTextView);
            cbTaskCompleted = itemView.findViewById(R.id.taskCheckBox); // <-- FIND THE CHECKBOX
        }

        // --- NEW METHOD to bind data and listeners to the view ---
        public void bind(final Task task, final OnTaskInteractionListener listener) {
            tvTaskTitle.setText(task.getTitle());
            cbTaskCompleted.setChecked(task.isCompleted()); // Set the checkbox based on the task's state

            // Add or remove strikethrough based on the task's state
            if (task.isCompleted()) {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Set a click listener on the checkbox
            cbTaskCompleted.setOnClickListener(v -> {
                // When clicked, call the onTaskChecked method from our interface
                if (listener != null) {
                    listener.onTaskChecked(task, cbTaskCompleted.isChecked());
                }
            });
        }
    }

    // --- (No changes to updateTasks method) ---
    public void updateTasks(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }
}
