package com.example.marqfit.ui.social;

import android.content.Context;

public class TimeAgo {
    public static String fromMillis(long nowMs, long pastMs) {
        long diff = Math.max(0, nowMs - pastMs);
        long sec = diff / 1000;
        long min = sec / 60;
        long hr  = min / 60;
        long day = hr / 24;
        if (day > 0) return day + "d ago";
        if (hr  > 0) return hr  + "h ago";
        if (min > 0) return min + "m ago";
        return "Just now";
    }
}