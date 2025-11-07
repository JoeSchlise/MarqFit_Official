package com.example.marqfit.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.marqfit.R;
import com.example.marqfit.databinding.FragmentDashboardBinding;
import com.example.marqfit.ui.workout.DayWorkoutsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // Live streak badge
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(auth.getUid())
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null || !snap.exists()) return;
                        Long curL = snap.getLong("currentStreak");
                        Long bestL = snap.getLong("longestStreak");
                        int cur = curL == null ? 0 : curL.intValue();
                        int best = bestL == null ? 0 : bestL.intValue();
                        TextView tv = view.findViewById(R.id.tvStreak);
                        if (tv != null) tv.setText("🔥 " + cur + "-day streak (best " + best + ")");
                    });
        }

        int[] ids = new int[] {
                R.id.daySun, R.id.dayMon, R.id.dayTue,
                R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat
        };
        List<Date> week = getCurrentWeekSundayToSaturday();
        for (int i = 0; i < ids.length; i++) {
            CardView card = view.findViewById(ids[i]);
            Date d = week.get(i);
            String dayIso  = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
            String weekKey = getIsoWeekKey(d);
            if (card != null) card.setOnClickListener(v -> openDay(weekKey, dayIso));
        }
    }

    private void openDay(String weekKey, String dayIso) {
        if (auth.getCurrentUser() == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (getContext() == null) return;

        Intent it = new Intent(getContext(), DayWorkoutsActivity.class);
        it.putExtra("WEEK_KEY", weekKey);
        it.putExtra("DAY_ISO", dayIso);
        startActivity(it);
    }

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
        if (week >= 52 && c.get(Calendar.MONTH) == Calendar.JANUARY) year--;
        return year + "-" + (week < 10 ? "0" + week : String.valueOf(week));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



