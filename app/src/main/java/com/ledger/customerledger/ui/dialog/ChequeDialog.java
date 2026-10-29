package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.DialogChequeBinding;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.ui.common.DatePicker;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

/** Add a cheque, or edit one (including its status). */
public final class ChequeDialog {
    private ChequeDialog() {}

    public static void show(final Context ctx, final long customerId, final Cheque existing, final Runnable onSaved) {
        final LedgerRepository repo = LedgerRepository.get(ctx);
        final DialogChequeBinding b = DialogChequeBinding.inflate(LayoutInflater.from(ctx));
        final boolean isNew = existing == null;

        DatePicker.attach(ctx, b.inputDate, isNew ? "" : existing.dueDate);
        if (!isNew) {
            b.inputAmount.setText(Money.plain(existing.amount));
            b.inputNumber.setText(existing.number);
            b.inputBank.setText(existing.bank);
            b.inputBill.setText(existing.bill);
            b.switchReceived.setChecked(existing.received);
            b.statusGroup.setVisibility(View.VISIBLE);
            Forms.setupSpinner(ctx, b.spinnerStatus,
                    new String[]{"Pending", "Cleared", "Bounced"}, existing.status.ordinal());
        }

        Runnable delete = isNew ? null : () -> {
            repo.deleteCheque(existing.id);
            onSaved.run();
        };

        FormDialog.show(ctx, isNew ? "New cheque" : "Edit cheque", b.getRoot(), () -> {
            if (!Forms.requireAmount(b.layoutAmount, b.inputAmount)) return false;
            if (b.switchReceived.isChecked() && !Forms.requireText(b.layoutDate, b.inputDate)) return false;

            Cheque ch = isNew ? new Cheque() : existing;
            ch.customerId = customerId;
            ch.amount = Money.parse(Forms.text(b.inputAmount));
            ch.number = Forms.text(b.inputNumber);
            ch.bank = Forms.text(b.inputBank);
            ch.dueDate = Forms.text(b.inputDate);
            ch.bill = Forms.text(b.inputBill);
            ch.received = b.switchReceived.isChecked();
            ch.status = isNew ? Cheque.Status.PENDING
                    : Cheque.Status.values()[b.spinnerStatus.getSelectedItemPosition()];
            if (ch.status == Cheque.Status.CLEARED) {
                ch.received = true;
                if (ch.clearedOn.isEmpty()) ch.clearedOn = Dates.today();
            } else {
                ch.clearedOn = "";
            }
            repo.saveCheque(ch);
            onSaved.run();
            return true;
        }, delete);
    }
}
