package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.model.BillDocument;
import com.ledger.customerledger.model.BillItem;
import com.ledger.customerledger.model.Purchase;
import com.ledger.customerledger.util.BillExport;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

import java.util.List;

/**
 * Shows a bill made on the Billing tab, with Print / Save PDF / Share.
 * justSaved = true right after saving. Otherwise a "Delete bill" button is shown.
 */
public final class BillDetailDialog {
    private BillDetailDialog() {}

    public static void show(Context ctx, Purchase bill, List<BillItem> items, Runnable onChanged) {
        show(ctx, bill, items, false, onChanged);
    }

    public static void show(final Context ctx, final Purchase bill, final List<BillItem> items,
                            boolean justSaved, final Runnable onChanged) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_bill, null);
        TextView text = view.findViewById(R.id.bill_text);

        final BillDocument doc = BillExport.create(ctx, bill, items);

        StringBuilder sb = new StringBuilder();
        sb.append(doc.customerName).append("\n").append(Dates.pretty(bill.date)).append("\n\n");
        for (BillItem it : items) {
            sb.append(it.name).append("\n   ")
                    .append(Money.plain(it.qty)).append(" x ").append(Money.format(it.price))
                    .append(" = ").append(Money.format(it.total())).append("\n");
        }
        sb.append("\nTotal: ").append(Money.format(bill.amount));
        if (doc.paidNow > 0) {
            sb.append("\nPaid now: ").append(Money.format(doc.paidNow));
            sb.append("\nBalance due: ").append(Money.format(doc.balanceDue()));
        }
        text.setText(sb.toString());

        final Uri[] saved = new Uri[1];

        view.findViewById(R.id.btn_print).setOnClickListener(v -> BillExport.print(ctx, doc));

        view.findViewById(R.id.btn_pdf).setOnClickListener(v -> {
            if (saved[0] == null) saved[0] = BillExport.savePdf(ctx, doc);
            Toast.makeText(ctx, saved[0] != null
                            ? "Saved to Downloads: " + doc.fileName() : "Could not save the PDF",
                    Toast.LENGTH_LONG).show();
        });

        view.findViewById(R.id.btn_share).setOnClickListener(v -> {
            if (saved[0] == null) saved[0] = BillExport.savePdf(ctx, doc);
            if (saved[0] == null) {
                Toast.makeText(ctx, "Could not create the PDF", Toast.LENGTH_LONG).show();
            } else {
                BillExport.share(ctx, saved[0], bill.bill);
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx)
                .setTitle(justSaved ? "Bill " + bill.bill + " saved" : "Bill " + bill.bill)
                .setView(view)
                .setPositiveButton("Close", null);

        if (!justSaved) {
            builder.setNeutralButton("Delete bill", (d, which) ->
                    ConfirmDialog.show(ctx, "Delete bill " + bill.bill,
                            "This removes the bill and its items from the customer's account.", "Delete", () -> {
                                LedgerRepository.get(ctx).deletePurchase(bill.id);
                                onChanged.run();
                            }));
        }
        builder.show();
    }
}