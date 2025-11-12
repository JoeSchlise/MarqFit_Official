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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private android.widget.CalendarView calendarView;
    private Set<String> datesWithWorkouts = new HashSet<>();

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
        db = FirebaseFirestore.getInstance();

        // Initialize calendar
        calendarView = view.findViewById(R.id.calendarView);
        setupCalendar();

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

            // Load workout history for calendar
            loadWorkoutHistory();
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

    private void setupCalendar() {
        if (calendarView == null) return;

        // Handle date selection
        calendarView.setOnDateChangeListener(new android.widget.CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull android.widget.CalendarView view, int year, int month, int dayOfMonth) {
                // Month is 0-indexed, so add 1
                String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                if (auth.getCurrentUser() == null) {
                    Toast.makeText(getContext(), "Please log in first", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show workout history for this date
                showWorkoutHistoryBottomSheet(dateStr);
            }
        });
    }

    private void loadWorkoutHistory() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getUid();

        // Query all weeks and days to find dates with workouts
        db.collection("users").document(uid)
                .collection("weeks")
                .get()
                .addOnSuccessListener(weekSnapshots -> {
                    datesWithWorkouts.clear();

                    for (QueryDocumentSnapshot weekDoc : weekSnapshots) {
                        String weekKey = weekDoc.getId();

                        // Get all days in this week
                        db.collection("users").document(uid)
                                .collection("weeks").document(weekKey)
                                .collection("days")
                                .get()
                                .addOnSuccessListener(daySnapshots -> {
                                    for (QueryDocumentSnapshot dayDoc : daySnapshots) {
                                        String dayIso = dayDoc.getId();

                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> exercises =
                                                (List<Map<String, Object>>) dayDoc.get("exercises");

                                        if (exercises != null && !exercises.isEmpty()) {
                                            datesWithWorkouts.add(dayIso);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load workout history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showWorkoutHistoryBottomSheet(String dayIso) {
        if (getContext() == null || auth.getCurrentUser() == null) return;

        String uid = auth.getUid();

        // Calculate week key for this date
        String weekKey = getWeekKeyFromIso(dayIso);

        // Create bottom sheet
        BottomSheetDialog bottomSheet = new BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_workout_history, null);
        bottomSheet.setContentView(sheetView);

        TextView dateTitle = sheetView.findViewById(R.id.dateTitle);
        TextView workoutList = sheetView.findViewById(R.id.workoutList);
        TextView emptyText = sheetView.findViewById(R.id.emptyText);
        View progressBar = sheetView.findViewById(R.id.progressBar);

        dateTitle.setText("Workouts for " + dayIso);

        // Check if this date has workouts
        boolean hasWorkouts = datesWithWorkouts.contains(dayIso);

        if (hasWorkouts) {
            dateTitle.setTextColor(getResources().getColor(R.color.lime_green));
        }

        // Fetch workouts for this date
        db.collection("users").document(uid)
                .collection("weeks").document(weekKey)
                .collection("days").document(dayIso)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (!snapshot.exists()) {
                        workoutList.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> exercises =
                            (List<Map<String, Object>>) snapshot.get("exercises");

                    if (exercises == null || exercises.isEmpty()) {
                        workoutList.setVisibility(View.GONE);
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Build workout summary
                    StringBuilder summary = new StringBuilder();
                    int totalMinutes = 0;
                    int completedCount = 0;

                    for (Map<String, Object> ex : exercises) {
                        String name = (String) ex.get("name");
                        Boolean completed = (Boolean) ex.get("completed");
                        Number mins = (Number) ex.get("minutes");

                        String status = (completed != null && completed) ? "✓" : "○";
                        summary.append(status).append(" ").append(name);

                        if (mins != null) {
                            summary.append(" (").append(mins.intValue()).append(" min)");
                            totalMinutes += mins.intValue();
                        }
                        summary.append("\n");

                        if (completed != null && completed) {
                            completedCount++;
                        }
                    }

                    summary.append("\n");
                    summary.append("━━━━━━━━━━━━━━━━━━━━\n");
                    summary.append("Total: ").append(exercises.size()).append(" exercises");
                    if (totalMinutes > 0) {
                        summary.append("\nDuration: ").append(totalMinutes).append(" minutes");
                    }
                    summary.append("\nCompleted: ").append(completedCount).append("/").append(exercises.size());

                    workoutList.setText(summary.toString());
                    workoutList.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    workoutList.setVisibility(View.GONE);
                    emptyText.setText("No workouts on this date");
                    emptyText.setVisibility(View.VISIBLE);
                });

        bottomSheet.show();
    }

    private String getWeekKeyFromIso(String dayIso) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(dayIso);
            if (date != null) {
                return getIsoWeekKey(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getIsoWeekKey(new Date());
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

    @Override
    public void onResume() {
        super.onResume();
        // Reload workout history when returning to this fragment
        if (auth != null && auth.getCurrentUser() != null) {
            loadWorkoutHistory();
        }
    }
}