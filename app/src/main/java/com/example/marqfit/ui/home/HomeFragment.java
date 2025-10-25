package com.example.marqfit.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marqfit.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI
    private EditText profileNameInput;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Keep your existing ViewModel usage (doesn't hurt anything)
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Optional: still observe and set the default text
        homeViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            if (binding != null && binding.textHome != null && TextUtils.isEmpty(binding.textHome.getText())) {
                binding.textHome.setText(text);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        profileNameInput = binding.getRoot().findViewById(com.example.marqfit.R.id.profileNameInput);

        // Load any saved name when the Home tab appears
        loadName();

        // Save button
        View saveBtn = binding.getRoot().findViewById(com.example.marqfit.R.id.saveNameButton);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> saveName());
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadName() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        DocumentReference userDoc = db.collection("users").document(uid);

        userDoc.get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String name = snap.getString("name");
                        if (!TextUtils.isEmpty(name)) {
                            if (profileNameInput != null) profileNameInput.setText(name);
                            binding.textHome.setText("Welcome, " + name + "!");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @SuppressLint("SetTextI18n")
    private void saveName() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = profileNameInput != null && profileNameInput.getText() != null
                ? profileNameInput.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(name)) {
            if (profileNameInput != null) {
                profileNameInput.setError("Enter your name");
                profileNameInput.requestFocus();
            }
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        DocumentReference userDoc = db.collection("users").document(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("updatedAt", System.currentTimeMillis());

        // MERGE so we don't wipe other fields you might add later
        userDoc.set(updates, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
                    binding.textHome.setText("Welcome, " + name + "!");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
