package com.ledger.customerledger.ui.dialog;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ConfirmDialog {
    private ConfirmDialog() {}

    public static void show(Context ctx, String title, String message, String confirmLabel, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(confirmLabel, (d, which) -> onConfirm.run())
                .show();
    }
}
