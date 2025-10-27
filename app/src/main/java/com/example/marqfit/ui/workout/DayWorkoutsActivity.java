package com.example.marqfit.ui.workout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marqfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
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

    private EditText inputName, inputMinutes;
    private Button btnQuickAdd;

    private DocumentReference dayDoc() {
        return db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso);
    }

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

        // Bind views
        inputName    = findViewById(R.id.inputName);
        inputMinutes = findViewById(R.id.inputMinutes);
        btnQuickAdd  = findViewById(R.id.btnQuickAdd);
        list         = findViewById(R.id.workoutsList);

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutsAdapter((position, checked) -> {
            // Update the model and persist the whole exercises array
            WorkoutsAdapter.Item it = adapter.data.get(position);
            it.completed = checked;
            saveAllExercises();
        });
        list.setAdapter(adapter);

        btnQuickAdd.setOnClickListener(v -> {
            String name = inputName.getText() == null ? "" : inputName.getText().toString().trim();
            String minStr = inputMinutes.getText() == null ? "" : inputMinutes.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                inputName.setError(getString(R.string.add_exercise));
                return;
            }
            Integer minutes = null;
            if (!TextUtils.isEmpty(minStr)) {
                try { minutes = Integer.parseInt(minStr); } catch (NumberFormatException ignored) {}
            }
            addExercise(name, minutes); // default completed = false
        });

        loadWorkouts();
    }

    @SuppressWarnings("unchecked")
    private void loadWorkouts() {
        if (uid == null) { adapter.set(new ArrayList<>()); return; }

        dayDoc().get().addOnSuccessListener(snap -> {
            List<WorkoutsAdapter.Item> items = new ArrayList<>();
            if (snap.exists()) {
                List<Map<String, Object>> ex = (List<Map<String, Object>>) snap.get("exercises");
                if (ex != null) {
                    for (Map<String, Object> m : ex) {
                        String name = (String) m.get("name");
                        Boolean completed = (Boolean) m.get("completed");
                        Number mins = (Number) m.get("minutes");
                        items.add(new WorkoutsAdapter.Item(
                                name == null ? "" : name,
                                completed != null && completed,
                                mins == null ? null : mins.intValue()
                        ));
                    }
                }
            }
            adapter.set(items);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void addExercise(String name, @Nullable Integer minutes) {
        if (uid == null) { Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("completed", false);
        if (minutes != null) item.put("minutes", minutes);

        Map<String, Object> data = new HashMap<>();
        data.put("exercises", FieldValue.arrayUnion(item));
        data.put("updatedAt", System.currentTimeMillis());

        dayDoc().set(data, SetOptions.merge())
                .addOnSuccessListener(x -> {
                    inputName.setText("");
                    inputMinutes.setText("");
                    loadWorkouts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /** Writes the adapter's current list back to Firestore (overwrites exercises array). */
    private void saveAllExercises() {
        if (uid == null) return;
        List<Map<String, Object>> ex = new ArrayList<>();
        for (WorkoutsAdapter.Item it : adapter.data) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", it.name);
            m.put("completed", it.completed);
            if (it.minutes != null) m.put("minutes", it.minutes);
            ex.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("exercises", ex);
        data.put("updatedAt", System.currentTimeMillis());

        dayDoc().set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // --- Adapter with checkbox ---
    static class WorkoutsAdapter extends RecyclerView.Adapter<WorkoutsAdapter.VH> {
        interface OnToggle { void onToggle(int position, boolean checked); }

        static class Item {
            String name; boolean completed; Integer minutes;
            Item(String n, boolean c, Integer m){ name = n; completed = c; minutes = m; }
        }

        final List<Item> data = new ArrayList<>();
        private final OnToggle onToggle;

        WorkoutsAdapter(OnToggle t){ this.onToggle = t; }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.CheckBox check;
            android.widget.TextView title, meta;
            VH(android.view.View v){
                super(v);
                check = v.findViewById(R.id.itemCheck);
                title = v.findViewById(R.id.itemTitle);
                meta  = v.findViewById(R.id.itemMeta);
            }
        }

        @NonNull
        @Override public VH onCreateViewHolder(android.view.ViewGroup p, int vtype) {
            android.view.View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_workout_simple, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            Item it = data.get(pos);
            h.title.setText(it.name);
            h.meta.setText(it.minutes != null ? it.minutes + " min" : "");
            // bind checkbox safely
            h.check.setOnCheckedChangeListener(null);
            h.check.setChecked(it.completed);
            h.check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                it.completed = isChecked;
                if (onToggle != null) onToggle.onToggle(h.getBindingAdapterPosition(), isChecked);
            });
        }

        @Override public int getItemCount(){ return data.size(); }

        @SuppressLint("NotifyDataSetChanged")
        void set(List<Item> items){
            data.clear();
            data.addAll(items);
            notifyDataSetChanged();
        }
    }
}




