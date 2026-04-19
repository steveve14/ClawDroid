package com.clawdroid.app.worker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.clawdroid.core.data.db.ClawDroidDatabase;

import java.time.Instant;

public class AutoDeleteWorker extends Worker {

    public AutoDeleteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("clawdroid_prefs", Context.MODE_PRIVATE);
        int days = prefs.getInt("auto_delete_days", 0);
        if (days <= 0) return Result.success();

        try {
            ClawDroidDatabase db = Room.databaseBuilder(
                    getApplicationContext(), ClawDroidDatabase.class, "clawdroid.db")
                    .build();
            String cutoffDate = Instant.now().minusSeconds((long) days * 86400).toString();
            db.conversationDao().deleteOlderThan(cutoffDate).blockingAwait();
            db.close();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
