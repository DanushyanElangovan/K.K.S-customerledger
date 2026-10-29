package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.DialogPaymentBinding;
import com.ledger.customerledger.model.Payment;
import com.ledger.customerledger.ui.common.DatePicker;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

import java.util.Arrays;

/** Money received from a customer. */
public final class PaymentDialog {
    private static final String[] METHODS = {"Cash", "Bank transfer", "Card", "Other"};

    private PaymentDialog() {}

    public static void show(final Context ctx, final long customerId, final Payment existing, final Runnable onSaved) {
        final LedgerRepository repo = LedgerRepository.get(ctx);
        final DialogPaymentBinding b = DialogPaymentBinding.inflate(LayoutInflater.from(ctx));
        final boolean isNew = existing == null;

        DatePicker.attach(ctx, b.inputDate, isNew ? Dates.today() : existing.date);
        int selected = 0;
        if (!isNew) {
            b.inputAmount.setText(Money.plain(existing.amount));
            b.inputNote.setText(existing.note);
            int i = Arrays.asList(METHODS).indexOf(existing.method);
            selected = i < 0 ? 0 : i;
        }
        Forms.setupSpinner(ctx, b.spinnerMethod, METHODS, selected);

        Runnable delete = isNew ? null : () -> {
            repo.deletePayment(existing.id);
            onSaved.run();
        };

        FormDialog.show(ctx, isNew ? "Amount received" : "Edit payment", b.getRoot(), () -> {
            if (!Forms.requireAmount(b.layoutAmount, b.inputAmount)) return false;
            Payment p = isNew ? new Payment() : existing;
            p.customerId = customerId;
            p.amount = Money.parse(Forms.text(b.inputAmount));
            p.date = Forms.text(b.inputDate);
            p.method = METHODS[b.spinnerMethod.getSelectedItemPosition()];
            p.note = Forms.text(b.inputNote);
            repo.savePayment(p);
            onSaved.run();
            return true;
        }, delete);
    }
}
