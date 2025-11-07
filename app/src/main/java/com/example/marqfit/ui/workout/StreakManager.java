package com.example.marqfit.ui.workout;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public class StreakManager {
    private static final String TAG = "StreakManager";
    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static void updateForDate(Context ctx, String uid, String dayIso, boolean allComplete) {
        if (uid == null || dayIso == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(uid);

        db.runTransaction(trx -> {
            Map<String, Object> user = trx.get(userRef).getData();
            if (user == null) user = new HashMap<>();

            Map<String, Object> completed = (Map<String, Object>) user.get("completedDates");
            if (completed == null) completed = new HashMap<>();

            boolean already = Boolean.TRUE.equals(completed.get(dayIso));
            if (allComplete) {
                if (!already) completed.put(dayIso, true);
            } else {
                if (already) completed.remove(dayIso);
            }

            int current = computeStreakFromToday(completed);
            Number longestPrev = (Number) user.get("longestStreak");
            int longest = Math.max(longestPrev == null ? 0 : longestPrev.intValue(), current);

            Map<String, Object> updates = new HashMap<>();
            updates.put("completedDates", completed);
            updates.put("currentStreak", current);
            updates.put("longestStreak", longest);

            trx.set(userRef, updates, com.google.firebase.firestore.SetOptions.merge());
            return null;
        }).addOnFailureListener(e -> Log.e(TAG, "updateForDate failed: " + e.getMessage()));
    }

    private static int computeStreakFromToday(Map<String, Object> completed) {
        if (completed == null || completed.isEmpty()) return 0;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);

        int streak = 0;
        for (int i = 0; i < 366; i++) {
            String iso = ISO.format(cal.getTime());
            if (Boolean.TRUE.equals(completed.get(iso))) {
                streak++;
                cal.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }
        return streak;
    }
}
