package com.clawdroid.core.ui;

import android.text.format.DateUtils;

import java.time.Instant;

public final class TimeUtils {

    private TimeUtils() {}

    public static CharSequence getRelativeTime(String isoTimestamp) {
        try {
            long millis = Instant.parse(isoTimestamp).toEpochMilli();
            return DateUtils.getRelativeTimeSpanString(
                    millis, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
        } catch (Exception e) {
            return "";
        }
    }
}
