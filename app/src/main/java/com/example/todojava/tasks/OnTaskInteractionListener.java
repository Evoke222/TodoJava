package com.example.todojava.tasks;

/**
 * This interface acts as a communication bridge between the TaskAdapter and the hosting Activity (FeedActivity).
 * It allows the adapter to report user interactions (like checking a box) back to the activity.
 */
public interface OnTaskInteractionListener {

    /**
     * Called when a task's checkbox state is changed by the user.
     * @param task The task object that was interacted with.
     * @param isChecked The new state of the checkbox (true if checked, false otherwise).
     */
    void onTaskChecked(Task task, boolean isChecked);

}
