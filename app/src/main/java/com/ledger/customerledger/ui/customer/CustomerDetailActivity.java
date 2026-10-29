package com.ledger.customerledger.ui.customer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.ActivityCustomerDetailBinding;
import com.ledger.customerledger.model.BillItem;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.model.Purchase;
import com.ledger.customerledger.model.LedgerEntry;
import com.ledger.customerledger.ui.adapter.ChequeAdapter;
import com.ledger.customerledger.ui.adapter.LedgerAdapter;
import com.ledger.customerledger.ui.cheque.ChequeActions;
import com.ledger.customerledger.ui.dialog.BillDetailDialog;
import com.ledger.customerledger.ui.dialog.ChequeDialog;
import com.ledger.customerledger.ui.dialog.ConfirmDialog;
import com.ledger.customerledger.ui.dialog.CustomerDialog;
import com.ledger.customerledger.ui.dialog.PaymentDialog;
import com.ledger.customerledger.ui.dialog.PurchaseDialog;
import com.ledger.customerledger.util.Money;

import java.util.List;

/** One customer: balance, add purchase / payment / cheque, cheques and account history. */
public class CustomerDetailActivity extends AppCompatActivity {

    private static final String EXTRA_CUSTOMER_ID = "customer_id";

    public static Intent intent(Context ctx, long customerId) {
        return new Intent(ctx, CustomerDetailActivity.class).putExtra(EXTRA_CUSTOMER_ID, customerId);
    }

    private ActivityCustomerDetailBinding b;
    private LedgerRepository repo;
    private long customerId;
    private CustomerAccount account;
    private ChequeAdapter chequeAdapter;
    private LedgerAdapter ledgerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        repo = LedgerRepository.get(this);
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, 0);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chequeAdapter = new ChequeAdapter(false, new ChequeActions(this, this::refresh));
        b.chequeList.setAdapter(chequeAdapter);
        ledgerAdapter = new LedgerAdapter(this::editEntry);
        b.historyList.setAdapter(ledgerAdapter);

        b.btnPurchase.setOnClickListener(v -> PurchaseDialog.show(this, customerId, null, this::refresh));
        b.btnPayment.setOnClickListener(v -> PaymentDialog.show(this, customerId, null, this::refresh));
        b.btnCheque.setOnClickListener(v -> ChequeDialog.show(this, customerId, null, this::refresh));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        account = repo.getAccount(customerId);
        if (account == null) { // deleted
            finish();
            return;
        }
        getSupportActionBar().setTitle(account.customer.name);

        double balance = account.getBalance();
        b.balanceLabel.setText(balance < 0 ? "CUSTOMER CREDIT" : "BALANCE TO BE PAID");
        b.balanceValue.setText(Money.format(Math.abs(balance)));
        b.purchasesValue.setText(Money.format(account.purchased));
        b.receivedValue.setText(Money.format(account.getPaid()));

        b.inHandGroup.setVisibility(account.inHand > 0 ? View.VISIBLE : View.GONE);
        b.inHandValue.setText(Money.format(account.inHand));
        b.afterValue.setText(Money.format(account.getBalanceAfterCheques()));
        b.awaitedGroup.setVisibility(account.awaited > 0 ? View.VISIBLE : View.GONE);
        b.awaitedValue.setText(Money.format(account.awaited));

        String phone = account.customer.phone;
        String note = account.customer.note;
        b.phoneText.setText((phone.isEmpty() ? "No phone" : "Phone " + phone) + (note.isEmpty() ? "" : " · " + note));

        List<com.ledger.customerledger.model.ChequeRow> cheques = repo.getOpenCheques(customerId);
        chequeAdapter.submit(cheques);
        b.chequeLabel.setVisibility(cheques.isEmpty() ? View.GONE : View.VISIBLE);
        b.chequeList.setVisibility(cheques.isEmpty() ? View.GONE : View.VISIBLE);

        List<LedgerEntry> history = repo.getHistory(customerId);
        ledgerAdapter.submit(history);
        b.emptyHistory.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Tapping a history line opens it for editing or deleting. */
    private void editEntry(LedgerEntry entry) {
        switch (entry.kind) {
            case PURCHASE:
                Purchase purchase = repo.getPurchase(entry.id);
                if (purchase == null) break;
                List<BillItem> items = repo.getBillItems(entry.id);
                if (items.isEmpty()) {
                    PurchaseDialog.show(this, customerId, purchase, this::refresh);   // typed-in purchase
                } else {
                    BillDetailDialog.show(this, purchase, items, this::refresh);      // bill from Billing tab
                }
                break;
            case PAYMENT:
                if (repo.getPayment(entry.id) != null)
                    PaymentDialog.show(this, customerId, repo.getPayment(entry.id), this::refresh);
                break;
            default:
                if (repo.getCheque(entry.id) != null)
                    ChequeDialog.show(this, customerId, repo.getCheque(entry.id), this::refresh);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.customer_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_edit) {
            CustomerDialog.show(this, account.customer, this::refresh, this::finish);
            return true;
        }
        if (id == R.id.action_delete) {
            ConfirmDialog.show(this, "Delete customer",
                    "Delete " + account.customer.name + " and all their purchases, payments and cheques? This cannot be undone.",
                    "Delete", () -> {
                        repo.deleteCustomer(customerId);
                        finish();
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}