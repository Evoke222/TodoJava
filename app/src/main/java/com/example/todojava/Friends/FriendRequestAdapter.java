package com.example.todojava.Friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todojava.R;
import com.example.todojava.models.FriendRequest; // You'll need this model
import com.example.todojava.models.User; // You will need a simple User model too

import java.util.List;
import java.util.Map;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {
    private final List<FriendRequest> requestList;
    private final Map<String, User> userMap; // Map to hold user data (UID -> User Object)
    private final OnRequestInteractionListener listener;

    // 1. Define an interface for click events
    public interface OnRequestInteractionListener {
        void onAcceptRequest(FriendRequest request);
        void onDeclineRequest(FriendRequest request);
    }

    public FriendRequestAdapter(List<FriendRequest> requestList, Map<String, User> userMap, OnRequestInteractionListener listener) {
        this.requestList = requestList;
        this.userMap = userMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the friend_request_item.xml layout you provided
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequest request = requestList.get(position);
        holder.bind(request, userMap.get(request.getFromUserId()), listener);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    // 2. Create the ViewHolder class
    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequesterName;
        Button buttonAccept, buttonDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the views from friend_request_item.xml
            tvRequesterName = itemView.findViewById(R.id.tvRequesterName);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonDecline = itemView.findViewById(R.id.buttonDecline);
        }

        public void bind(final FriendRequest request, final User requester, final OnRequestInteractionListener listener) {
            // Display the requester's username
            if (requester != null) {
                tvRequesterName.setText(requester.getUsername());
            } else {
                tvRequesterName.setText("Loading..."); // Or a placeholder
            }

            // Set the click listeners
            buttonAccept.setOnClickListener(v -> listener.onAcceptRequest(request));
            buttonDecline.setOnClickListener(v -> listener.onDeclineRequest(request));
        }
    }
}
