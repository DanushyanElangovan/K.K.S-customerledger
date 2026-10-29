package com.ledger.customerledger.ui.cheque;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.ui.customer.CustomerDetailActivity;
import com.ledger.customerledger.ui.dialog.ChequeDialog;
import com.ledger.customerledger.ui.dialog.ReminderDialog;

/**
 * Everything a cheque card's buttons do. Used by the Home, Cheques and customer screens,
 * so the behaviour lives in one place. onChanged redraws the calling screen.
 */
public class ChequeActions {
    private final Context ctx;
    private final LedgerRepository repo;
    private final Runnable onChanged;

    public ChequeActions(Context ctx, Runnable onChanged) {
        this.ctx = ctx;
        this.repo = LedgerRepository.get(ctx);
        this.onChanged = onChanged;
    }

    public void received(Cheque c) {
        repo.markReceived(c.id);
        done("Marked as received");
    }

    public void cleared(Cheque c) {
        repo.setStatus(c.id, Cheque.Status.CLEARED);
        done("Cheque cleared. Balance updated");
    }

    public void bounced(Cheque c) {
        repo.setStatus(c.id, Cheque.Status.BOUNCED);
        done("Marked as bounced");
    }

    public void reopen(Cheque c) {
        repo.setStatus(c.id, Cheque.Status.PENDING);
        done("Cheque reopened");
    }

    public void remind(ChequeRow row) {
        ReminderDialog.show(ctx, row);
    }

    public void edit(Cheque c) {
        ChequeDialog.show(ctx, c.customerId, c, onChanged);
    }

    public void openCustomer(long customerId) {
        ctx.startActivity(CustomerDetailActivity.intent(ctx, customerId));
    }

    private void done(String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
        onChanged.run();
    }
}
