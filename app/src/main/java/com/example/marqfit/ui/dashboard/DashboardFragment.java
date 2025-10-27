package com.example.marqfit.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.marqfit.R;
import com.example.marqfit.databinding.FragmentDashboardBinding;
import com.example.marqfit.ui.workout.DayWorkoutsActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(DashboardViewModel.class);
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // Wire the 7 day buttons (vertical list) to open the DayWorkouts screen
        int[] ids = new int[] {
                R.id.daySun, R.id.dayMon, R.id.dayTue,
                R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat
        };

        List<Date> week = getCurrentWeekSundayToSaturday();
        for (int i = 0; i < ids.length; i++) {
            Button b = view.findViewById(ids[i]);
            Date d = week.get(i);
            String dayIso  = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d); // e.g., 2025-10-25
            String weekKey = getIsoWeekKey(d);                                        // e.g., 2025-43
            b.setOnClickListener(v -> openDay(weekKey, dayIso));
        }
    }

    private void openDay(String weekKey, String dayIso) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent it = new Intent(requireContext(), DayWorkoutsActivity.class);
        it.putExtra("WEEK_KEY", weekKey);
        it.putExtra("DAY_ISO", dayIso);
        startActivity(it);
    }

    // --- Week helpers ---
    private List<Date> getCurrentWeekSundayToSaturday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        List<Date> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(c.getTime());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private String getIsoWeekKey(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        int year = c.get(Calendar.YEAR);
        return year + "-" + (week < 10 ? "0" + week : String.valueOf(week));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

