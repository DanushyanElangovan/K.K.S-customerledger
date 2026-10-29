package com.ledger.customerledger.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.DialogCustomerBinding;
import com.ledger.customerledger.model.Customer;
import com.ledger.customerledger.ui.common.Forms;

/** Add a customer, or edit one. Delete is offered only when onDeleted is given. */
public final class CustomerDialog {
    private CustomerDialog() {}

    public static void show(final Context ctx, final Customer existing,
                            final Runnable onSaved, final Runnable onDeleted) {
        final LedgerRepository repo = LedgerRepository.get(ctx);
        final DialogCustomerBinding b = DialogCustomerBinding.inflate(LayoutInflater.from(ctx));

        if (existing != null) {
            b.inputName.setText(existing.name);
            b.inputPhone.setText(existing.phone);
            b.inputNote.setText(existing.note);
        }

        Runnable delete = (existing != null && onDeleted != null) ? () -> {
            repo.deleteCustomer(existing.id);
            onDeleted.run();
        } : null;

        FormDialog.show(ctx, existing == null ? "New customer" : "Edit customer", b.getRoot(), () -> {
            if (!Forms.requireText(b.layoutName, b.inputName)) return false;
            Customer c = existing != null ? existing : new Customer();
            c.name = Forms.text(b.inputName);
            c.phone = Forms.text(b.inputPhone);
            c.note = Forms.text(b.inputNote);
            repo.saveCustomer(c);
            onSaved.run();
            return true;
        }, delete);
    }
}
