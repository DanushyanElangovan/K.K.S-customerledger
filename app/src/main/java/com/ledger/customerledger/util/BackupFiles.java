package com.ledger.customerledger.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.ledger.customerledger.data.LedgerRepository;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Saves a backup of all data to Downloads, shares it, and remembers when it was last done. */
public final class BackupFiles {
    private BackupFiles() {}

    private static final String PREFS = "backup";
    private static final String KEY_LAST = "last_backup";

    /** Returns the Uri of the saved backup, or null if it failed. */
    public static Uri save(Context ctx) {
        try {
            String name = "KKS-Backup-"
                    + new SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(new Date()) + ".db";
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, name);
            v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return null;
            try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                if (out == null) return null;
                LedgerRepository.get(ctx).backupTo(out);
            }
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST, System.currentTimeMillis()).apply();
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    /** Opens the share sheet so the file can be sent to Google Drive, WhatsApp, email... */
    public static void share(Context ctx, Uri backup) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/octet-stream");
        send.putExtra(Intent.EXTRA_STREAM, backup);
        send.putExtra(Intent.EXTRA_SUBJECT, "K.K.S Enterprises backup");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(send, "Send backup to"));
    }

    /** "Last backup: 10 Oct 2026" or a warning if never. */
    public static String lastBackupText(Context ctx) {
        long t = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0);
        if (t == 0) return "No backup yet. Tap Backup and send the file to Google Drive.";
        long days = (System.currentTimeMillis() - t) / 86_400_000L;
        String when = new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(t));
        return "Last backup: " + when + (days >= 7 ? " (" + days + " days ago, back up again)" : "");
    }
}