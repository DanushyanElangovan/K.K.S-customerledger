package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.DialogPurchaseBinding;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.Purchase;
import com.ledger.customerledger.ui.common.DatePicker;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

/**
 * New or edited purchase. For a new purchase you can also record the cheque given for the bill:
 * "received" (in your hands) or "not received yet" (expected).
 */
public final class PurchaseDialog {
    private static final int NO_CHEQUE = 0;
    private static final int CHEQUE_RECEIVED = 1;
    private static final int CHEQUE_EXPECTED = 2;

    private PurchaseDialog() {}

    public static void show(final Context ctx, final long customerId, final Purchase existing, final Runnable onSaved) {
        final LedgerRepository repo = LedgerRepository.get(ctx);
        final DialogPurchaseBinding b = DialogPurchaseBinding.inflate(LayoutInflater.from(ctx));
        final boolean isNew = existing == null;

        DatePicker.attach(ctx, b.inputDate, isNew ? Dates.today() : existing.date);
        DatePicker.attach(ctx, b.inputChequeDate, "");
        if (!isNew) {
            b.inputBill.setText(existing.bill);
            b.inputAmount.setText(Money.plain(existing.amount));
            b.inputNote.setText(existing.note);
            b.chequeSection.setVisibility(View.GONE);
        }

        Forms.setupSpinner(ctx, b.chequeMode, new String[]{
                "No cheque (credit or cash)", "Cheque received", "Cheque not received yet"}, NO_CHEQUE);
        b.chequeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                b.chequeGroup.setVisibility(position == NO_CHEQUE ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Runnable delete = isNew ? null : () -> {
            repo.deletePurchase(existing.id);
            onSaved.run();
        };

        FormDialog.show(ctx, isNew ? "New purchase" : "Edit purchase", b.getRoot(), () -> {
            if (!Forms.requireAmount(b.layoutAmount, b.inputAmount)) return false;
            int mode = b.chequeMode.getSelectedItemPosition();
            if (isNew && mode == CHEQUE_RECEIVED && !Forms.requireText(b.layoutChequeDate, b.inputChequeDate)) {
                return false;
            }

            Purchase p = isNew ? new Purchase() : existing;
            p.customerId = customerId;
            p.date = Forms.text(b.inputDate);
            p.bill = Forms.text(b.inputBill);
            p.amount = Money.parse(Forms.text(b.inputAmount));
            p.note = Forms.text(b.inputNote);
            repo.savePurchase(p);

            if (isNew && mode != NO_CHEQUE) {
                Cheque ch = new Cheque();
                ch.customerId = customerId;
                ch.purchaseId = p.id;
                ch.number = Forms.text(b.inputChequeNo);
                ch.bank = Forms.text(b.inputChequeBank);
                ch.dueDate = Forms.text(b.inputChequeDate);
                ch.amount = p.amount;
                ch.bill = p.bill;
                ch.received = mode == CHEQUE_RECEIVED;
                ch.status = Cheque.Status.PENDING;
                repo.saveCheque(ch);
            }
            onSaved.run();
            return true;
        }, delete);
    }
}
