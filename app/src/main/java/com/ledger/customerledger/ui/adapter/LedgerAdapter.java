package com.ledger.customerledger.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ledger.customerledger.R;
import com.ledger.customerledger.databinding.ItemLedgerBinding;
import com.ledger.customerledger.model.LedgerEntry;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/** The account history lines of one customer. */
public class LedgerAdapter extends RecyclerView.Adapter<LedgerAdapter.Holder> {

    public interface Listener {
        void onClick(LedgerEntry entry);
    }

    private final Listener listener;
    private List<LedgerEntry> items = new ArrayList<>();

    public LedgerAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<LedgerEntry> list) {
        items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLedgerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final LedgerEntry e = items.get(position);
        ItemLedgerBinding b = h.binding;

        b.entryTitle.setText(e.title);
        b.entrySub.setText(Dates.pretty(e.date) + (e.subtitle.isEmpty() ? "" : " · " + e.subtitle));
        boolean adds = e.amount > 0;
        b.entryAmount.setText((adds ? "+ " : "− ") + Money.format(Math.abs(e.amount)));
        b.entryAmount.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), adds ? R.color.bad : R.color.good));
        b.entryBalance.setText("Bal " + Money.format(e.runningBalance));
        b.getRoot().setOnClickListener(v -> listener.onClick(e));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemLedgerBinding binding;

        Holder(ItemLedgerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
