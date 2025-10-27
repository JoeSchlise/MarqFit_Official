package com.example.marqfit.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marqfit.R;
import com.example.marqfit.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ProgressBar busyMeterBar = binding.busyMeterBar;
        TextView busyMeterText = binding.busyMeterText;

        int busyPercent = 65;
        busyMeterBar.setProgress(busyPercent);
        busyMeterText.setText("Current: " + busyPercent + "% full");

        // Initialize Firebase instances here
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Corrected placement

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}





