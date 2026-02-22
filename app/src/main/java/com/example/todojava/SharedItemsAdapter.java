package com.example.todojava;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todojava.tasks.Task;
import java.util.List;
import java.util.Map;

public class SharedItemsAdapter extends RecyclerView.Adapter<SharedItemsAdapter.SharedItemViewHolder> {

    private final List<Task> sharedItems;
    private final Map<String, String> ownerNames; // Map from owner UID to username
    private final OnSharedItemInteractionListener listener;

    public interface OnSharedItemInteractionListener {
        void onAcceptItem(Task item);
        void onDeclineItem(Task item);
    }

    public SharedItemsAdapter(List<Task> sharedItems, Map<String, String> ownerNames, OnSharedItemInteractionListener listener) {
        this.sharedItems = sharedItems;
        this.ownerNames = ownerNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SharedItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shared_item_row, parent, false);
        return new SharedItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SharedItemViewHolder holder, int position) {
        Task item = sharedItems.get(position);
        holder.bind(item, ownerNames.get(item.getOwner_uid()), listener);
    }

    @Override
    public int getItemCount() {
        return sharedItems.size();
    }

    static class SharedItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvSharedItemTitle;
        TextView tvSharedFrom;
        ImageButton buttonAccept;
        ImageButton buttonDecline;

        SharedItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSharedItemTitle = itemView.findViewById(R.id.tvSharedItemTitle);
            tvSharedFrom = itemView.findViewById(R.id.tvSharedFrom);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonDecline = itemView.findViewById(R.id.buttonDecline);
        }

        void bind(final Task item, String ownerName, final OnSharedItemInteractionListener listener) {
            tvSharedItemTitle.setText(item.getTitle());
            tvSharedFrom.setText("Shared by: " + (ownerName != null ? ownerName : "Unknown"));

            buttonAccept.setOnClickListener(v -> listener.onAcceptItem(item));
            buttonDecline.setOnClickListener(v -> listener.onDeclineItem(item));
        }
    }
}
