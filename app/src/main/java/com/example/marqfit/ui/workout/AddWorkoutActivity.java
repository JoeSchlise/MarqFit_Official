package com.example.marqfit.ui.workout;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.example.marqfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AddWorkoutActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid, weekKey, dayIso;

    private DocumentReference dayDoc() {
        return db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        weekKey = getIntent().getStringExtra("WEEK_KEY");
        dayIso  = getIntent().getStringExtra("DAY_ISO");
        setTitle(getString(R.string.add_exercise));

        EditText input = findViewById(R.id.inputName);
        Button save = findViewById(R.id.btnSave);




        // ✅ Handle Save button
        save.setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                input.setError(getString(R.string.add_exercise));
                return;
            }

            // Append to exercises array on the day doc
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("completed", false);

            Map<String, Object> data = new HashMap<>();
            data.put("exercises", FieldValue.arrayUnion(item));
            data.put("updatedAt", System.currentTimeMillis());

            dayDoc().set(data, SetOptions.merge())
                    .addOnSuccessListener(x -> {
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                        finish(); // Closes and returns to previous screen
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
