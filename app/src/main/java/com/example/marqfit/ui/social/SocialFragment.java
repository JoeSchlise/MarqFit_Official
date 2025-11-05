package com.example.marqfit.ui.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marqfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SocialFragment extends Fragment {

    private RecyclerView socialFeedRecycler;
    private SocialFeedAdapter adapter;
    private List<WorkoutPost> feedList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Button signOutButton, addFriendButton;
    private EditText searchFriendInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_social, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Find UI elements
        socialFeedRecycler = view.findViewById(R.id.socialFeedRecycler);
        signOutButton = view.findViewById(R.id.signOutButton);
        addFriendButton = view.findViewById(R.id.addFriendButton);
        searchFriendInput = view.findViewById(R.id.searchFriendInput);

        // Setup feed list
        socialFeedRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SocialFeedAdapter(feedList);
        socialFeedRecycler.setAdapter(adapter);

        // Load feed from Firestore
        loadFeed();

        // ✅ Setup button listeners
        signOutButton.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

            // Go back to Login screen
            Intent intent = new Intent(getActivity(), com.example.marqfit.ui.Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        addFriendButton.setOnClickListener(v -> {
            String username = searchFriendInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(getContext(), "Enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Example add-friend logic (you can customize this later)
            db.collection("users").document(auth.getUid())
                    .collection("friends")
                    .document(username)
                    .set(new java.util.HashMap<String, Object>() {{
                        put("username", username);
                    }})
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "Friend added: " + username, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error adding friend", Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    private void loadFeed() {
        db.collection("workout_feed")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    feedList.clear();

                    for (DocumentSnapshot doc : snapshots) {
                        String username = doc.getString("username");
                        String action = doc.getString("action");
                        com.google.firebase.Timestamp time = doc.getTimestamp("timestamp");

                        feedList.add(new WorkoutPost(username, action, time, null));
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
