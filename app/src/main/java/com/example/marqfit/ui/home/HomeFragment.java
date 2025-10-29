package com.example.marqfit.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.os.Handler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marqfit.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final Set<String> ADMIN_UIDS = new HashSet<>(List.of(
            "MxNbQH5AuxfUtjJMF6uzhhUU7ga2"
    ));
    private boolean isAdmin = false;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        db.collection("gymStatus").document("busyMeter")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    Number n = (Number) snap.get("value");
                    int busy = n == null ? 0 : Math.max(0, Math.min(100, n.intValue()));
                    binding.busyMeterBar.setProgress(busy);
                    binding.busyMeterText.setText("Current: " + busy + "% full");
                    if (isAdmin) binding.busyInput.setText(String.valueOf(busy));
                });

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        isAdmin = uid != null && ADMIN_UIDS.contains(uid);
        setAdminUI(isAdmin);

        binding.busySaveBtn.setOnClickListener(v -> {
            if (!isAdmin) return;
            String s = binding.busyInput.getText() == null ? "" : binding.busyInput.getText().toString().trim();
            if (TextUtils.isEmpty(s)) {
                binding.busyInput.setError("Required");
                return;
            }
            int val;
            try {
                val = Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                binding.busyInput.setError("Enter 0–100");
                return;
            }
            val = Math.max(0, Math.min(100, val));

            Map<String, Object> data = new HashMap<>();
            data.put("value", val);
            data.put("updatedAt", System.currentTimeMillis());
            data.put("updatedBy", uid);

            db.collection("gymStatus").document("busyMeter").set(data)
                    .addOnSuccessListener(x -> Toast.makeText(getContext(), "Updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(err -> Toast.makeText(getContext(), "Denied: " + err.getMessage(), Toast.LENGTH_LONG).show());
        });
        final Handler clockHandler = new Handler();
        final Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                String currentTime = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                        .format(new Date());
                binding.clockText.setText(currentTime);
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
        return root;
    }

    private void setAdminUI(boolean enabled) {
        binding.busyInput.setVisibility(enabled ? View.VISIBLE : View.GONE);
        binding.busySaveBtn.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}





