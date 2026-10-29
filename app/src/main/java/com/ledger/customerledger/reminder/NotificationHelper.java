package com.ledger.customerledger.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.ui.main.MainActivity;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

import java.util.List;

public final class NotificationHelper {
    private static final String CHANNEL_ID = "cheques";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationHelper() {}

    /**
     * Shows one notification listing cheques that are due soon or overdue.
     * With force=true it also shows a message when nothing is due (used by the test button).
     */
    public static void showDueCheques(Context ctx, boolean force) {
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ChequeRow> due = LedgerRepository.get(ctx).getDueSoon();
        if (due.isEmpty() && !force) return;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Cheque reminders", NotificationManager.IMPORTANCE_HIGH));

        Notification.InboxStyle style = new Notification.InboxStyle();
        for (int i = 0; i < due.size() && i < 6; i++) style.addLine(describe(due.get(i)));

        String title = due.isEmpty()
                ? "No cheques due in the next 2 days"
                : due.size() + (due.size() == 1 ? " cheque" : " cheques") + " to remind customers about";
        String text = due.isEmpty() ? "Reminders are working" : describe(due.get(0));

        PendingIntent open = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(style)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        nm.notify(NOTIFICATION_ID, n);
    }

    private static String describe(ChequeRow row) {
        Cheque c = row.cheque;
        return row.customerName + ": " + Money.format(c.amount) + ", "
                + Dates.dueInfo(c.dueDate).text.toLowerCase()
                + (c.received ? "" : " (not received)");
    }
}
