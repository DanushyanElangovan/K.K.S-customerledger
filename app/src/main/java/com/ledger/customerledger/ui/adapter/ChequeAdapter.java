package com.ledger.customerledger.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ledger.customerledger.databinding.ItemChequeBinding;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.ui.cheque.ChequeActions;
import com.ledger.customerledger.ui.common.ChipStyle;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/** One card per cheque. The buttons shown depend on the cheque's status. */
public class ChequeAdapter extends RecyclerView.Adapter<ChequeAdapter.Holder> {

    private final boolean showCustomerName;
    private final ChequeActions actions;
    private List<ChequeRow> rows = new ArrayList<>();

    public ChequeAdapter(boolean showCustomerName, ChequeActions actions) {
        this.showCustomerName = showCustomerName;
        this.actions = actions;
    }

    public void submit(List<ChequeRow> list) {
        rows = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemChequeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final ChequeRow row = rows.get(position);
        final Cheque c = row.cheque;
        ItemChequeBinding b = h.binding;

        b.title.setText(showCustomerName ? row.customerName : "Cheque" + (c.number.isEmpty() ? "" : " " + c.number));
        b.title.setOnClickListener(showCustomerName ? v -> actions.openCustomer(c.customerId) : null);

        List<String> parts = new ArrayList<>();
        if (!c.number.isEmpty() && showCustomerName) parts.add("No. " + c.number);
        if (!c.bank.isEmpty()) parts.add(c.bank);
        if (!c.bill.isEmpty()) parts.add("Bill " + c.bill);
        b.subtitle.setText(parts.isEmpty() ? "No details yet" : String.join(" · ", parts));
        b.amount.setText(Money.format(c.amount));

        // chips
        b.chipDate.setText(Dates.pretty(c.dueDate));
        if (c.status == Cheque.Status.PENDING) {
            Dates.DueInfo due = Dates.dueInfo(c.dueDate);
            ChipStyle.Tone tone = due.level == Dates.Level.BAD ? ChipStyle.Tone.BAD
                    : due.level == Dates.Level.WARN ? ChipStyle.Tone.WARN : ChipStyle.Tone.NEUTRAL;
            ChipStyle.apply(b.chipDue, due.text, tone);
            b.chipDue.setVisibility(View.VISIBLE);
            ChipStyle.apply(b.chipStatus, c.received ? "In hand" : "Not received yet", ChipStyle.Tone.NEUTRAL);
        } else {
            b.chipDue.setVisibility(View.GONE);
            if (c.status == Cheque.Status.CLEARED) {
                ChipStyle.apply(b.chipStatus, "Cleared", ChipStyle.Tone.GOOD);
            } else {
                ChipStyle.apply(b.chipStatus, "Bounced", ChipStyle.Tone.BAD);
            }
        }

        // buttons
        b.btnEdit.setOnClickListener(v -> actions.edit(c));
        if (c.status == Cheque.Status.PENDING) {
            b.btnPrimary.setText(c.received ? "Cleared" : "Received");
            b.btnPrimary.setOnClickListener(v -> {
                if (c.received) actions.cleared(c); else actions.received(c);
            });
            b.btnBounced.setVisibility(c.received ? View.VISIBLE : View.GONE);
            b.btnBounced.setOnClickListener(v -> actions.bounced(c));
            b.btnRemind.setVisibility(View.VISIBLE);
            b.btnRemind.setOnClickListener(v -> actions.remind(row));
        } else {
            b.btnPrimary.setText("Reopen");
            b.btnPrimary.setOnClickListener(v -> actions.reopen(c));
            b.btnBounced.setVisibility(View.GONE);
            b.btnRemind.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemChequeBinding binding;

        Holder(ItemChequeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
