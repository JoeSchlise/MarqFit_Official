package com.example.marqfit.ui.workout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import java.util.Map;

public class DayPlanActivity extends AppCompatActivity {

    private RecyclerView list;
    private EditText inputExercise;
    private Button addBtn, saveBtn;
    private ExerciseAdapter adapter;

    private FirebaseFirestore db;
    private String uid, weekKey, dayIso;

    private DocumentReference dayDoc() {
        return db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso);
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_plan);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        weekKey = getIntent().getStringExtra("WEEK_KEY");
        dayIso  = getIntent().getStringExtra("DAY_ISO");
        setTitle("Plan: " + dayIso);

        list = findViewById(R.id.exerciseList);
        inputExercise = findViewById(R.id.exerciseInput);
        addBtn = findViewById(R.id.addExerciseButton);
        saveBtn = findViewById(R.id.saveDayButton);

        adapter = new ExerciseAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);


        // swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                adapter.removeAt(pos);
                autoSave();
            }
        }).attachToRecyclerView(list);

        addBtn.setOnClickListener(v -> {
            String name = inputExercise.getText() == null ? "" : inputExercise.getText().toString().trim();
            if (TextUtils.isEmpty(name)) { inputExercise.setError("Enter exercise"); return; }
            adapter.addItem(new ExerciseItem(name, false));
            inputExercise.setText("");
            autoSave();
        });

        saveBtn.setOnClickListener(v -> saveDay());

        loadDay();
    }

    @SuppressWarnings("unchecked")
    private void loadDay() {
        if (uid == null) return;
        dayDoc().get().addOnSuccessListener(snap -> {
            List<ExerciseItem> items = new ArrayList<>();
            if (snap.exists()) {
                List<Map<String, Object>> ex = (List<Map<String, Object>>) snap.get("exercises");
                if (ex != null) {
                    for (Map<String, Object> m : ex) {
                        String n = (String) m.get("name");
                        Boolean c = (Boolean) m.get("completed");
                        items.add(new ExerciseItem(n == null ? "" : n, c != null && c));
                    }
                }
            }
            adapter.setItems(items);
        });
    }

    private void saveDay() { writeDay(true); }
    private void autoSave() { writeDay(false); }



    private void writeDay(boolean toast) {
        if (uid == null) return;
        List<Map<String, Object>> ex = new ArrayList<>();
        for (ExerciseItem it : adapter.getItems()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", it.name);
            m.put("completed", it.completed);
            ex.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("exercises", ex);
        data.put("updatedAt", System.currentTimeMillis());

        dayDoc().set(data, SetOptions.merge())
                .addOnSuccessListener(v -> { if (toast) Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // --- RecyclerView types ---
    static class ExerciseItem {
        String name; boolean completed;
        ExerciseItem(String n, boolean c){ name = n; completed = c; }
    }

    static class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.VH> {
        private final List<ExerciseItem> data = new ArrayList<>();

        static class VH extends RecyclerView.ViewHolder {
            CheckBox cb; TextView name; View root;
            VH(View v){ super(v); root=v; cb=v.findViewById(R.id.checkBox); name=v.findViewById(R.id.exerciseName); }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vtype) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_exercise, p, false);
            return new VH(v);
        }


        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            ExerciseItem it = data.get(pos);
            h.name.setText(it.name);
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(it.completed);

            // green when checked
            h.root.setBackgroundResource(it.completed ? R.color.lime_green : android.R.color.transparent);

            h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                it.completed = isChecked;
                h.root.setBackgroundResource(isChecked ? R.color.lime_green : android.R.color.transparent);
                ((AppCompatActivity) h.itemView.getContext()).runOnUiThread(() -> {
                    if (h.itemView.getContext() instanceof DayPlanActivity) {
                        ((DayPlanActivity) h.itemView.getContext()).autoSave();
                    }
                });
            });

            // rename on long-press
            h.root.setOnLongClickListener(v1 -> {
                EditText input = new EditText(v1.getContext());
                input.setText(it.name);
                new androidx.appcompat.app.AlertDialog.Builder(v1.getContext())
                        .setTitle("Rename exercise")
                        .setView(input)
                        .setPositiveButton("Save", (d, w) -> { it.name = input.getText().toString().trim(); notifyItemChanged(h.getBindingAdapterPosition()); ((DayPlanActivity) h.itemView.getContext()).autoSave(); })
                        .setNegativeButton("Cancel", (d, w) -> notifyItemChanged(h.getBindingAdapterPosition()))
                        .show();
                return true;
            });
        }

        @Override public int getItemCount(){ return data.size(); }
        void addItem(ExerciseItem it){ data.add(it); notifyItemInserted(data.size()-1); }
        @SuppressLint("NotifyDataSetChanged")
        void setItems(List<ExerciseItem> items){ data.clear(); data.addAll(items); notifyDataSetChanged(); }
        List<ExerciseItem> getItems(){ return data; }
        void removeAt(int pos){ if (pos>=0 && pos<data.size()){ data.remove(pos); notifyItemRemoved(pos);} }
    }
}

