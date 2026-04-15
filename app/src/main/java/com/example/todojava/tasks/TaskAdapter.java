package com.example.todojava.tasks;

import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.todojava.R;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskInteractionListener listener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUid = FirebaseAuth.getInstance().getUid();

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
        holder.bind(task, listener, db, currentUid);
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
        Chip chipRemindMe;
        
        ImageView ivOwner;
        ImageView ivSharedWith;
        TextView tvMemberNames;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.taskTitleTextView);
            tvTaskDueDate = itemView.findViewById(R.id.taskDueDateTextView);
            tvTaskDetails = itemView.findViewById(R.id.taskDetailsTextView);
            tvTaskType = itemView.findViewById(R.id.taskTypeTextView);
            cbTaskCompleted = itemView.findViewById(R.id.taskCheckBox);
            deleteButton = itemView.findViewById(R.id.button_delete_task);
            chipRemindMe = itemView.findViewById(R.id.chipRemindMe);
            
            ivOwner = itemView.findViewById(R.id.ivOwner);
            ivSharedWith = itemView.findViewById(R.id.ivSharedWith);
            tvMemberNames = itemView.findViewById(R.id.tvMemberNames);
        }

        public void bind(final Task task, final OnTaskInteractionListener listener, FirebaseFirestore db, String currentUid) {
            tvTaskTitle.setText(task.getTitle());
            tvTaskDueDate.setText(task.getDueDate());
            tvTaskDetails.setText(task.getDetails());
            cbTaskCompleted.setChecked(task.isCompleted());

            // Bind remindMe status
            if (task.isRemindMe()) {
                chipRemindMe.setVisibility(View.VISIBLE);
            } else {
                chipRemindMe.setVisibility(View.GONE);
            }

            // Reset visibility
            ivSharedWith.setVisibility(View.GONE);
            tvMemberNames.setText("");

            // Load Owner and Shared With Info
            loadMembersInfo(task, db, currentUid);

            if (task.getType() != null && !task.getType().isEmpty()) {
                String type = task.getType();
                tvTaskType.setText(type.substring(0, 1).toUpperCase() + type.substring(1));
            } else {
                tvTaskType.setText("Task");
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

        private void loadMembersInfo(Task task, FirebaseFirestore db, String currentUid) {
            final String[] ownerName = {"..."};
            final String[] sharedName = {""};

            // Load Owner
            db.collection("users").document(task.getOwner_uid()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String username = doc.getString("username");
                    String pfp = doc.getString("pfp_url");
                    ownerName[0] = (task.getOwner_uid().equals(currentUid)) ? "You" : username;
                    
                    Glide.with(itemView.getContext()).load(pfp).placeholder(R.drawable.ic_default_profile).circleCrop().into(ivOwner);
                    updateMemberNames(ownerName[0], sharedName[0]);
                }
            });

            // Load Shared With (if exists)
            if (task.getSharedWithUid() != null && !task.getSharedWithUid().isEmpty()) {
                ivSharedWith.setVisibility(View.VISIBLE);
                db.collection("users").document(task.getSharedWithUid()).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String pfp = doc.getString("pfp_url");
                        sharedName[0] = (task.getSharedWithUid().equals(currentUid)) ? "You" : username;

                        Glide.with(itemView.getContext()).load(pfp).placeholder(R.drawable.ic_default_profile).circleCrop().into(ivSharedWith);
                        updateMemberNames(ownerName[0], sharedName[0]);
                    }
                });
            }
        }

        private void updateMemberNames(String owner, String shared) {
            if (shared.isEmpty()) {
                tvMemberNames.setText(owner);
            } else {
                tvMemberNames.setText(owner + " + " + shared);
            }
        }
    }
}
