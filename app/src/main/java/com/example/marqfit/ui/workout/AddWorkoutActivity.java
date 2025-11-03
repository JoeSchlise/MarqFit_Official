package com.example.marqfit.ui.workout;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marqfit.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddWorkoutActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid, weekKey, dayIso;

    private DocumentReference dayDoc() {
        return db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso);
    }

    static class Template {
        final String name; final String tags; final Integer minutes; final String videoUrl;
        Template(String n, String t, Integer m, String v){ name=n; tags=t; minutes=m; videoUrl=v; }
    }

    static class TplAdapter extends RecyclerView.Adapter<TplAdapter.VH> {
        interface OnPick { void onPick(int pos); }
        final List<Template> data = new ArrayList<>();
        int selected = -1; final OnPick onPick;
        TplAdapter(OnPick cb){ onPick = cb; }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, tags; RadioButton radio;
            VH(View v){
                super(v);
                title = v.findViewById(R.id.tplTitle);
                tags  = v.findViewById(R.id.tplTags);
                radio = v.findViewById(R.id.tplRadio);
            }
        }

        @Override public VH onCreateViewHolder(ViewGroup p, int viewType) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_workout_template, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            Template tpl = data.get(pos);
            h.title.setText(tpl.name);
            h.tags.setText(tpl.tags);
            h.radio.setOnCheckedChangeListener(null);
            h.radio.setChecked(pos == selected);

            View.OnClickListener pick = v -> {
                int old = selected;
                selected = h.getBindingAdapterPosition();
                if (old != -1) notifyItemChanged(old);
                notifyItemChanged(selected);
                if (onPick != null) onPick.onPick(selected);
            };
            h.itemView.setOnClickListener(pick);
            h.radio.setOnClickListener(pick);
        }

        @Override public int getItemCount(){ return data.size(); }
        Template getSelected(){ return (selected>=0 && selected<data.size()) ? data.get(selected) : null; }
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        weekKey = getIntent().getStringExtra("WEEK_KEY");
        dayIso  = getIntent().getStringExtra("DAY_ISO");
        setTitle(getString(R.string.add_workout_title));

        // Header back text
        TextView back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.templateList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        MaterialButton btnAdd    = findViewById(R.id.btnAddToPlan);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        TplAdapter ad = new TplAdapter(hasSel -> btnAdd.setEnabled(true));
        rv.setAdapter(ad);

        // Bodyweight
        ad.data.add(new Template("Push-Ups (3×12)", "bodyweight • push", 10, "https://youtu.be/IODxDxX7oi4"));
        ad.data.add(new Template("Pull-Ups (3×8)", "bodyweight • pull", 10, "https://youtu.be/eGo4IYlbE5g"));
        ad.data.add(new Template("Dips (3×10)", "bodyweight • push", 10, "https://youtu.be/2z8JmcrW-As"));
        ad.data.add(new Template("Plank (3×60s)", "core • hold", 8,  "https://youtu.be/pSHjTRCQxIw"));
        ad.data.add(new Template("Mountain Climbers (3×45s)", "cardio • core", 8, "https://youtu.be/cnyTQDSE884"));
        ad.data.add(new Template("Burpees (3×12)", "hiit • bodyweight", 10, "https://youtu.be/auBLPXO8Fww"));

        // Lower Body
        ad.data.add(new Template("Bulgarian Split Squat (3×12/leg)", "legs • unilateral", 15, "https://youtu.be/2C-uNgKwPLE"));
        ad.data.add(new Template("Barbell Back Squat (4×8)", "legs • strength", 18, "https://youtu.be/ultWZbUMPL8"));
        ad.data.add(new Template("Romanian Deadlift (3×8)", "hamstrings • posterior", 15, "https://youtu.be/5Is9DYkX3yE"));
        ad.data.add(new Template("Hip Thrust (3×10)", "glutes", 14, "https://youtu.be/LM8XHLYJoYs"));
        ad.data.add(new Template("Walking Lunges (3×12/leg)", "legs • unilateral", 12, "https://youtu.be/1J4hRICM0xE"));
        ad.data.add(new Template("Calf Raise (4×12)", "calves", 10, "https://youtu.be/ymqg6v1Z5i8"));

        // Upper – Push
        ad.data.add(new Template("Bench Press (4×8)", "chest • push", 18, "https://youtu.be/gRVjAtPip0Y"));
        ad.data.add(new Template("Incline Dumbbell Press (3×10)", "chest • push", 14, "https://youtu.be/8iPEnn-ltC8"));
        ad.data.add(new Template("Overhead Press (3×8)", "shoulders • push", 12, "https://youtu.be/F3QY5vMz_6I"));
        ad.data.add(new Template("Lateral Raise (3×15)", "shoulders", 10, "https://youtu.be/3VcKaXpzqRo"));
        ad.data.add(new Template("Triceps Pushdown (3×12)", "triceps", 9, "https://youtu.be/2-LAMcpzODU"));

        // Upper – Pull
        ad.data.add(new Template("Bent-Over Row (3×10)", "back • pull", 14, "https://youtu.be/vT2GjY_Umpw"));
        ad.data.add(new Template("Lat Pulldown (3×10)", "back • lats", 12, "https://youtu.be/CAwf7n6Luuc"));
        ad.data.add(new Template("Seated Cable Row (3×12)", "back", 12, "https://youtu.be/GZbfZ033f74"));
        ad.data.add(new Template("Face Pulls (3×15)", "rear delts • upper back", 10, "https://youtu.be/Rep-qVOkqgk"));
        ad.data.add(new Template("Hammer Curls (3×12)", "biceps", 9, "https://youtu.be/CFBZ4jN1CMI"));

        // Core & Cardio
        ad.data.add(new Template("Hanging Leg Raise (3×12)", "core • abs", 10, "https://youtu.be/HDntl7yzzVI"));
        ad.data.add(new Template("Cable Crunch (3×15)", "core • abs", 9, "https://youtu.be/2J-eQyS5jZs"));
        ad.data.add(new Template("Treadmill – Incline Walk (20 min, Incline 10)", "cardio • treadmill", 20, "https://youtu.be/rjF5wZ0JQdA"));
        ad.data.add(new Template("Rowing Machine (15 min steady)", "cardio • row", 15, "https://youtu.be/6bTq2G9mYxk"));

        ad.notifyDataSetChanged();

        // Save selection
        btnAdd.setOnClickListener(v -> {
            if (uid == null) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }
            Template chosen = ad.getSelected();
            if (chosen == null) return;

            Map<String, Object> item = new HashMap<>();
            item.put("name", chosen.name);
            item.put("completed", false);
            if (chosen.minutes != null) item.put("minutes", chosen.minutes);
            if (chosen.videoUrl != null) item.put("videoUrl", chosen.videoUrl);

            Map<String, Object> data = new HashMap<>();
            data.put("exercises", FieldValue.arrayUnion(item));
            data.put("updatedAt", System.currentTimeMillis());

            dayDoc().set(data, SetOptions.merge())
                    .addOnSuccessListener(x -> { setResult(RESULT_OK); finish(); })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}


