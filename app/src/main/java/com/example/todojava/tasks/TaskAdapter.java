package com.example.todojava.tasks;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> newTaskList) {
        this.taskList.clear();
        this.taskList.addAll(newTaskList);
        notifyDataSetChanged();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        TextView tvTaskDueDate;
        TextView tvTaskDetails;
        TextView tvTaskType;
        CheckBox cbTaskCompleted;
        ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.taskTitleTextView);
            tvTaskDueDate = itemView.findViewById(R.id.taskDueDateTextView);
            tvTaskDetails = itemView.findViewById(R.id.taskDetailsTextView);
            tvTaskType = itemView.findViewById(R.id.taskTypeTextView);
            cbTaskCompleted = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.button_delete_task);
        }

        public void bind(final Task task, final OnTaskInteractionListener listener) {
            tvTaskTitle.setText(task.getTitle());
            tvTaskDueDate.setText(task.getDueDate());
            tvTaskDetails.setText(task.getDetails());
            cbTaskCompleted.setChecked(task.isCompleted());

            if (task.getType() != null && !task.getType().isEmpty()) {
                String type = task.getType();
                tvTaskType.setText(type.substring(0, 1).toUpperCase() + type.substring(1));
            } else {
                tvTaskType.setText("Task"); // Default to task if not specified
            }

            if (task.isCompleted()) {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            cbTaskCompleted.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskChecked(task, cbTaskCompleted.isChecked());
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Delete " + (task.getType() != null ? task.getType() : "item"))
                            .setMessage("Are you sure you want to delete this " + (task.getType() != null ? task.getType() : "item") + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                listener.onTaskDeleted(task);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }
    }
}
