package com.ledger.customerledger.reminder;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ledger.customerledger.AppConfig;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    private static final String WORK_NAME = "daily-cheque-check";

    private ReminderScheduler() {}

    /** Runs the cheque check every day at about AppConfig.REMINDER_HOUR. */
    public static void scheduleDaily(Context context) {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, AppConfig.REMINDER_HOUR);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.before(now)) next.add(Calendar.DAY_OF_YEAR, 1);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(next.getTimeInMillis() - now.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}
