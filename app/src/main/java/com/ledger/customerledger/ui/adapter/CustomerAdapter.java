package com.ledger.customerledger.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ledger.customerledger.R;
import com.ledger.customerledger.databinding.ItemCustomerBinding;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/** One card per customer: name, phone, balance and an optional Delete button. */
public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.Holder> {

    public interface Listener {
        void onOpen(CustomerAccount account);
        void onDelete(CustomerAccount account);
    }

    private final Listener listener;
    private final boolean showDelete;
    private List<CustomerAccount> items = new ArrayList<>();

    public CustomerAdapter(boolean showDelete, Listener listener) {
        this.showDelete = showDelete;
        this.listener = listener;
    }

    public void submit(List<CustomerAccount> list) {
        items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCustomerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final CustomerAccount a = items.get(position);
        ItemCustomerBinding b = h.binding;

        b.customerName.setText(a.customer.name);
        String sub = a.customer.phone.isEmpty() ? "No phone" : a.customer.phone;
        if (a.pendingCheques > 0) {
            sub += " · " + a.pendingCheques + (a.pendingCheques == 1 ? " cheque pending" : " cheques pending");
        }
        b.customerSub.setText(sub);

        double balance = a.getBalance();
        if (balance > 0) {
            b.balance.setText(Money.format(balance));
            b.balance.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.bad));
        } else {
            b.balance.setText(balance < 0 ? "Credit " + Money.format(-balance) : "Settled");
            b.balance.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.good));
        }

        b.getRoot().setOnClickListener(v -> listener.onOpen(a));
        b.btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        b.btnDelete.setOnClickListener(v -> listener.onDelete(a));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemCustomerBinding binding;

        Holder(ItemCustomerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
