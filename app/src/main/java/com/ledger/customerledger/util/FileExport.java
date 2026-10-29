package com.ledger.customerledger.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class FileExport {
    private FileExport() {}

    /** Saves text into the phone's Downloads folder. Returns true on success. */
    public static boolean saveToDownloads(Context ctx, String fileName, String mime, String content) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                if (out == null) return false;
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
