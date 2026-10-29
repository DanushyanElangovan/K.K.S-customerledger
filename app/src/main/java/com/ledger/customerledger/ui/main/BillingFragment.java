package com.ledger.customerledger.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.FragmentBillingBinding;
import com.ledger.customerledger.databinding.ItemBillLineBinding;
import com.ledger.customerledger.model.BillItem;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.model.Purchase;
import com.ledger.customerledger.ui.adapter.ItemSuggestionAdapter;
import com.ledger.customerledger.ui.common.DatePicker;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.ui.common.SimpleTextWatcher;
import com.ledger.customerledger.ui.dialog.BillDetailDialog;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.ItemCatalog;
import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/**
 * Billing tab: pick a customer, add items, save. The bill is stored on the customer's account
 * as a purchase (so their balance goes up), together with its items.
 * Optional: money paid now (stored as a payment) and a cheque for the rest.
 * After saving, a window opens with Print / Save PDF / Share.
 */
public class BillingFragment extends RefreshableFragment {

    private static final int NO_CHEQUE = 0;
    private static final int CHEQUE_RECEIVED = 1;
    private static final int CHEQUE_EXPECTED = 2;

    private FragmentBillingBinding b;
    private LedgerRepository repo;
    private List<CustomerAccount> customers = new ArrayList<>();
    private final List<ItemBillLineBinding> lines = new ArrayList<>();
    private long selectedCustomerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentBillingBinding.inflate(inflater, container, false);
        repo = LedgerRepository.get(requireContext());

        DatePicker.attach(requireContext(), b.inputChequeDate, "");
        Forms.setupSpinner(requireContext(), b.chequeMode, new String[]{
                "No cheque (credit or cash)", "Cheque received", "Cheque not received yet"}, NO_CHEQUE);
        b.chequeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                b.chequeGroup.setVisibility(position == NO_CHEQUE ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        b.customerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < customers.size()) selectedCustomerId = customers.get(position).customer.id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        b.inputPaid.addTextChangedListener(new SimpleTextWatcher(this::updateTotals));
        b.btnAddItem.setOnClickListener(v -> addLine());
        b.btnSave.setOnClickListener(v -> save());

        resetForm();
        return b.getRoot();
    }

    /** Reloads the customer list (a customer may have been added) without touching the form. */
    @Override
    protected void refresh() {
        if (b == null) return;
        customers = repo.getAccounts(null);
        String[] names = new String[customers.size()];
        int selected = 0;
        for (int i = 0; i < customers.size(); i++) {
            names[i] = customers.get(i).customer.name;
            if (customers.get(i).customer.id == selectedCustomerId) selected = i;
        }
        Forms.setupSpinner(requireContext(), b.customerSpinner, names, selected);

        boolean hasCustomers = !customers.isEmpty();
        b.customerSpinner.setEnabled(hasCustomers);
        b.btnSave.setEnabled(hasCustomers);
        b.noCustomersHint.setVisibility(hasCustomers ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------ item lines

    private void addLine() {
        final ItemBillLineBinding line = ItemBillLineBinding.inflate(getLayoutInflater(), b.itemsContainer, false);
        // products from assets/items.txt; anything else can simply be typed
        line.inputItem.setAdapter(new ItemSuggestionAdapter(requireContext(), ItemCatalog.names(requireContext())));
        line.inputQty.setText("1");
        line.inputQty.addTextChangedListener(new SimpleTextWatcher(this::updateTotals));
        line.inputPrice.addTextChangedListener(new SimpleTextWatcher(this::updateTotals));
        line.btnRemove.setOnClickListener(v -> {
            b.itemsContainer.removeView(line.getRoot());
            lines.remove(line);
            updateTotals();
        });
        b.itemsContainer.addView(line.getRoot());
        lines.add(line);
        updateTotals();
    }

    private static double lineTotal(ItemBillLineBinding l) {
        return Money.round(Money.number(Forms.text(l.inputQty)) * Money.parse(Forms.text(l.inputPrice)));
    }

    private double billTotal() {
        double total = 0;
        for (ItemBillLineBinding l : lines) total += lineTotal(l);
        return Money.round(total);
    }

    private void updateTotals() {
        if (b == null) return;
        for (ItemBillLineBinding l : lines) l.lineTotal.setText(Money.format(lineTotal(l)));
        double total = billTotal();
        double paid = Money.parse(Forms.text(b.inputPaid));
        b.totalValue.setText(Money.format(total));
        b.leftHint.setText("Left to pay: " + Money.format(Math.max(total - paid, 0)));
    }

    // ------------------------------------------------------------ save

    private void save() {
        if (customers.isEmpty()) return;
        CustomerAccount account = customers.get(b.customerSpinner.getSelectedItemPosition());

        List<BillItem> items = new ArrayList<>();
        for (ItemBillLineBinding l : lines) {
            String name = Forms.text(l.inputItem);
            double qty = Money.number(Forms.text(l.inputQty));
            double price = Money.parse(Forms.text(l.inputPrice));
            if (name.isEmpty() && price == 0) continue;           // blank row, ignore
            if (name.isEmpty()) { l.inputItem.setError("Enter the item"); return; }
            if (qty <= 0) { l.inputQty.setError("Qty?"); return; }
            items.add(new BillItem(name, qty, price));
        }
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }
        double total = 0;
        for (BillItem it : items) total += it.total();
        total = Money.round(total);
        if (total <= 0) {
            Toast.makeText(requireContext(), "The bill total is zero", Toast.LENGTH_SHORT).show();
            return;
        }

        double paid = Money.parse(Forms.text(b.inputPaid));
        if (paid > total) {
            b.inputPaid.setError("More than the bill total");
            return;
        }
        double left = Money.round(total - paid);

        String billNo = Forms.text(b.inputBillNo);
        if (billNo.isEmpty()) billNo = repo.nextBillNumber();

        Cheque cheque = null;
        int mode = b.chequeMode.getSelectedItemPosition();
        if (mode != NO_CHEQUE) {
            if (left <= 0) {
                Toast.makeText(requireContext(), "Nothing is left to pay by cheque", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mode == CHEQUE_RECEIVED && !Forms.requireText(b.layoutChequeDate, b.inputChequeDate)) return;
            cheque = new Cheque();
            cheque.number = Forms.text(b.inputChequeNo);
            cheque.bank = Forms.text(b.inputChequeBank);
            cheque.dueDate = Forms.text(b.inputChequeDate);
            cheque.amount = left;
            cheque.bill = billNo;
            cheque.received = mode == CHEQUE_RECEIVED;
            cheque.status = Cheque.Status.PENDING;
        }

        Purchase bill = new Purchase();
        bill.customerId = account.customer.id;
        bill.date = Forms.text(b.inputDate);
        bill.bill = billNo;
        bill.amount = total;
        bill.note = summary(items);
        repo.saveBill(bill, items, paid, cheque);

        resetForm();
        onDataChanged();
        // Opens the saved bill with Print / Save PDF / Share
        BillDetailDialog.show(requireContext(), bill, items, true, this::onDataChanged);
    }

    private static String summary(List<BillItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(items.size()).append(items.size() == 1 ? " item: " : " items: ");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i).name);
        }
        return sb.length() > 80 ? sb.substring(0, 77) + "..." : sb.toString();
    }

    private void resetForm() {
        lines.clear();
        b.itemsContainer.removeAllViews();
        addLine();
        b.inputBillNo.setText(repo.nextBillNumber());
        DatePicker.attach(requireContext(), b.inputDate, Dates.today());
        b.inputPaid.setText("");
        b.inputPaid.setError(null);
        b.chequeMode.setSelection(NO_CHEQUE);
        b.inputChequeNo.setText("");
        b.inputChequeBank.setText("");
        b.inputChequeDate.setText("");
        updateTotals();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}