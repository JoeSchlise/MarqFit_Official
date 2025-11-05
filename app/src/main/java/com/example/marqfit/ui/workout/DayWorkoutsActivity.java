package com.example.marqfit.ui.workout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marqfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DayWorkoutsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid, weekKey, dayIso;

    private RecyclerView list;
    private WorkoutsAdapter adapter;

    private DocumentReference dayDoc() {
        return db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_workouts);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        weekKey = getIntent().getStringExtra("WEEK_KEY");
        dayIso  = getIntent().getStringExtra("DAY_ISO");
        setTitle(String.format(Locale.US, "Workouts: %s", dayIso));

        // RecyclerView
        list = findViewById(R.id.workoutsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutsAdapter(
                // checkbox toggled
                (position, checked) -> {
                    WorkoutsAdapter.Item it = adapter.data.get(position);
                    it.completed = checked;
                    saveAllExercises();
                },
                this::openYoutubeExternal
        );
        list.setAdapter(adapter);

        // Add workout screen (your existing picker that saves videoUrl)
        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddWorkoutActivity.class)
                        .putExtra("WEEK_KEY", weekKey)
                        .putExtra("DAY_ISO", dayIso)));

        loadWorkouts();
    }

    @Override protected void onResume() {
        super.onResume();
        loadWorkouts();
    }

    @SuppressWarnings("unchecked")
    private void loadWorkouts() {
        if (uid == null) { adapter.set(new ArrayList<>()); toggleEmpty(true); return; }

        dayDoc().get().addOnSuccessListener(snap -> {
            List<WorkoutsAdapter.Item> items = new ArrayList<>();
            if (snap.exists()) {
                List<Map<String, Object>> ex = (List<Map<String, Object>>) snap.get("exercises");
                if (ex != null) {
                    for (Map<String, Object> m : ex) {
                        String name = (String) m.get("name");
                        Boolean completed = (Boolean) m.get("completed");
                        Number mins = (Number) m.get("minutes");
                        String video = (String) m.get("videoUrl");
                        items.add(new WorkoutsAdapter.Item(
                                name == null ? "" : name,
                                completed != null && completed,
                                mins == null ? null : mins.intValue(),
                                video
                        ));
                    }
                }
            }
            adapter.set(items);
            toggleEmpty(items.isEmpty());
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void toggleEmpty(boolean show) {
        View empty = findViewById(R.id.emptyState);
        if (empty != null) empty.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void saveAllExercises() {
        if (uid == null) return;
        List<Map<String, Object>> ex = new ArrayList<>();
        for (WorkoutsAdapter.Item it : adapter.data) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", it.name);
            m.put("completed", it.completed);
            if (it.minutes != null) m.put("minutes", it.minutes);
            if (it.videoUrl != null && !it.videoUrl.trim().isEmpty()) m.put("videoUrl", it.videoUrl);
            ex.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("exercises", ex);
        data.put("updatedAt", System.currentTimeMillis());

        dayDoc().set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        checkAndPostIfAllComplete();
    }
    private void checkAndPostIfAllComplete() {
        if (uid == null) return;

        dayDoc().get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exercises =
                    (List<Map<String, Object>>) snapshot.get("exercises");

            if (exercises == null || exercises.isEmpty()) return;

            boolean allComplete = true;
            for (Map<String, Object> ex : exercises) {
                Boolean done = (Boolean) ex.get("completed");
                if (done == null || !done) {
                    allComplete = false;
                    break;
                }
            }

            if (allComplete) {
                postToSocialFeed(dayIso);
            }
        });
    }
    private void postToSocialFeed(String dayLabel) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;

        String username = auth.getCurrentUser().getDisplayName();
        if (username == null || username.isEmpty()) username = "User";

        Map<String, Object> post = new HashMap<>();
        post.put("username", username);
        post.put("action", "completed all workouts for " + dayLabel + " 💪");
        post.put("timestamp", com.google.firebase.Timestamp.now());
        post.put("userId", auth.getUid());

        db.collection("workout_feed")
                .add(post)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Workout posted to feed!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error posting to feed", Toast.LENGTH_SHORT).show());
    }
    private void openYoutubeExternal(@Nullable String urlOrId){
        if (urlOrId == null || urlOrId.trim().isEmpty()) return;

        String url = urlOrId;
        if (urlOrId.matches("^[A-Za-z0-9_-]{11}$")) {
            url = "https://www.youtube.com/watch?v=" + urlOrId;
        }

        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        it.setPackage("com.google.android.youtube");
        try {
            startActivity(it);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    static class WorkoutsAdapter extends RecyclerView.Adapter<WorkoutsAdapter.VH> {
        interface OnToggle { void onToggle(int position, boolean checked); }
        interface OnWatch { void open(@Nullable String videoUrl); }

        static class Item {
            String name; boolean completed; Integer minutes; String videoUrl;
            Item(String n, boolean c, Integer m, String v){ name = n; completed = c; minutes = m; videoUrl = v; }
        }

        final List<Item> data = new ArrayList<>();
        private final OnToggle onToggle;
        private final OnWatch onWatch;

        WorkoutsAdapter(OnToggle t, OnWatch w){
            this.onToggle = t; this.onWatch = w;
        }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.CheckBox check;
            android.widget.TextView title, meta, watch;
            VH(View v){
                super(v);
                check = v.findViewById(R.id.itemCheck);
                title = v.findViewById(R.id.itemTitle);
                meta  = v.findViewById(R.id.itemMeta);
                watch = v.findViewById(R.id.itemWatch);
            }
        }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vtype) {
            View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_workout_simple, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Item it = data.get(pos);
            h.title.setText(it.name);
            h.meta.setText(it.minutes != null ? it.minutes + " min" : "");

            if (it.videoUrl != null && !it.videoUrl.trim().isEmpty()) {
                h.watch.setVisibility(View.VISIBLE);
                h.watch.setOnClickListener(v -> { if (onWatch != null) onWatch.open(it.videoUrl); });
            } else {
                h.watch.setVisibility(View.GONE);
                h.watch.setOnClickListener(null);
            }

            h.check.setOnCheckedChangeListener(null);
            h.check.setChecked(it.completed);
            h.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                it.completed = isChecked;
                if (onToggle != null) onToggle.onToggle(h.getBindingAdapterPosition(), isChecked);
            });

            h.itemView.setOnLongClickListener(null);
        }

        @Override public int getItemCount(){ return data.size(); }

        @SuppressLint("NotifyDataSetChanged")
        void set(List<Item> items){ data.clear(); data.addAll(items); notifyDataSetChanged(); }
    }
}







