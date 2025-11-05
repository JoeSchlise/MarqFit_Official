package com.example.marqfit.ui.social;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marqfit.R;

import java.util.List;

/**
 * Adapter for displaying posts in the Social feed (like Venmo's activity feed).
 */
public class SocialFeedAdapter extends RecyclerView.Adapter<SocialFeedAdapter.ViewHolder> {

    private final List<WorkoutPost> feedList;

    public SocialFeedAdapter(List<WorkoutPost> feedList) {
        this.feedList = feedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the feed item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutPost post = feedList.get(position);

        holder.usernameText.setText(post.getUsername());
        holder.actionText.setText(post.getAction());

        // Convert timestamp to "time ago" text
        if (post.getTimestamp() != null) {
            long postTime = post.getTimestamp().toDate().getTime();
            String timeAgo = TimeAgo.fromMillis(System.currentTimeMillis(), postTime);
            holder.timeText.setText(timeAgo);
        } else {
            holder.timeText.setText("");
        }

        // Load profile image using Glide (or default icon if null)
        if (post.getProfileUrl() != null && !post.getProfileUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getProfileUrl())
                    .placeholder(R.drawable.ic_people)
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_people);
        }
    }

    @Override
    public int getItemCount() {
        return feedList.size();
    }

    /**
     * ViewHolder holds all views for a single feed item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView usernameText;
        TextView actionText;
        TextView timeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            usernameText = itemView.findViewById(R.id.usernameText);
            actionText = itemView.findViewById(R.id.actionText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}
