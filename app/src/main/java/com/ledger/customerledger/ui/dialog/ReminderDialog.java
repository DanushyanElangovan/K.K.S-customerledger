package com.ledger.customerledger.ui.dialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ledger.customerledger.databinding.DialogReminderBinding;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.util.Phone;
import com.ledger.customerledger.util.ReminderText;

/** Shows a ready-made message you can copy, send on WhatsApp, or use while calling the customer. */
public final class ReminderDialog {
    private ReminderDialog() {}

    public static void show(final Context ctx, ChequeRow row) {
        final DialogReminderBinding b = DialogReminderBinding.inflate(LayoutInflater.from(ctx));
        final String phone = row.customerPhone;
        final boolean hasPhone = !phone.isEmpty();

        b.textPhone.setText(hasPhone ? "Phone: " + phone : "No phone number saved. Add one on the customer page.");
        b.inputMessage.setText(ReminderText.build(row));
        b.btnWhatsapp.setEnabled(hasPhone);
        b.btnCall.setEnabled(hasPhone);

        b.btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("reminder", Forms.text(b.inputMessage)));
            Toast.makeText(ctx, "Message copied", Toast.LENGTH_SHORT).show();
        });
        b.btnWhatsapp.setOnClickListener(v -> open(ctx, Uri.parse("https://wa.me/"
                + Phone.forWhatsApp(phone) + "?text=" + Uri.encode(Forms.text(b.inputMessage)))));
        b.btnCall.setOnClickListener(v -> {
            try {
                ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
            } catch (Exception e) {
                Toast.makeText(ctx, "Cannot open the dialer", Toast.LENGTH_SHORT).show();
            }
        });

        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Remind " + row.customerName)
                .setView(b.getRoot())
                .setPositiveButton("Close", null)
                .show();
    }

    private static void open(Context ctx, Uri uri) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(ctx, "Nothing on this phone can open that", Toast.LENGTH_SHORT).show();
        }
    }
}
