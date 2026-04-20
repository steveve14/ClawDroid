package com.clawdroid.app.worker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.clawdroid.core.data.db.ClawDroidDatabase;
import com.clawdroid.core.data.db.DatabaseKeyManager;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

public class AutoDeleteWorker extends Worker {

    static {
        System.loadLibrary("sqlcipher");
    }

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
            // SEC-H1: Worker도 동일한 암호화 DB를 사용해야 한다.
            char[] passphrase = DatabaseKeyManager.getOrCreatePassphrase(getApplicationContext());
            byte[] passBytes = new String(passphrase).getBytes(StandardCharsets.UTF_8);
            Arrays.fill(passphrase, '0');

            ClawDroidDatabase db = Room.databaseBuilder(
                    getApplicationContext(), ClawDroidDatabase.class, "clawdroid.db")
                    .openHelperFactory(new SupportOpenHelperFactory(passBytes))
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
