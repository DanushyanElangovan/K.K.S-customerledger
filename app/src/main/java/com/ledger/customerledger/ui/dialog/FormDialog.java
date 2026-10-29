package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.BooleanSupplier;

/**
 * Shared shell for every form: Save / Cancel and an optional Delete.
 * onSave returns true when the data was valid and saved (the dialog then closes),
 * or false to keep the dialog open so the user can fix an error.
 */
public final class FormDialog {
    private FormDialog() {}

    public static void show(final Context ctx, String title, View content,
                            final BooleanSupplier onSave, final Runnable onDelete) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(content)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);
        if (onDelete != null) builder.setNeutralButton("Delete", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (onSave.getAsBoolean()) dialog.dismiss();
            });
            if (onDelete != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        ConfirmDialog.show(ctx, "Delete this?", "This cannot be undone.", "Delete", () -> {
                            onDelete.run();
                            dialog.dismiss();
                        }));
            }
        });
        dialog.show();
    }
}
