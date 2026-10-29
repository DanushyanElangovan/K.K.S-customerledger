package com.ledger.customerledger.ui.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.FragmentCustomersBinding;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.ui.adapter.CustomerAdapter;
import com.ledger.customerledger.ui.common.Forms;
import com.ledger.customerledger.ui.customer.CustomerDetailActivity;
import com.ledger.customerledger.ui.dialog.ConfirmDialog;
import com.ledger.customerledger.ui.dialog.CustomerDialog;

import java.util.List;

/** The customer list with search, add and delete. */
public class CustomersFragment extends RefreshableFragment {

    private FragmentCustomersBinding b;
    private CustomerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentCustomersBinding.inflate(inflater, container, false);

        adapter = new CustomerAdapter(true, new CustomerAdapter.Listener() {
            @Override
            public void onOpen(CustomerAccount a) {
                startActivity(CustomerDetailActivity.intent(requireContext(), a.customer.id));
            }

            @Override
            public void onDelete(CustomerAccount a) {
                confirmDelete(a);
            }
        });
        b.customerList.setAdapter(adapter);

        b.btnAdd.setOnClickListener(v ->
                CustomerDialog.show(requireContext(), null, this::onDataChanged, null));
        b.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });
        return b.getRoot();
    }

    private void confirmDelete(final CustomerAccount a) {
        String message = "Delete " + a.customer.name
                + " and all their purchases, payments and cheques? This cannot be undone.";
        ConfirmDialog.show(requireContext(), "Delete customer", message, "Delete", () -> {
            LedgerRepository.get(requireContext()).deleteCustomer(a.customer.id);
            onDataChanged();
        });
    }

    @Override
    protected void refresh() {
        if (b == null) return;
        String query = Forms.text(b.searchInput);
        List<CustomerAccount> list = LedgerRepository.get(requireContext()).getAccounts(query);
        adapter.submit(list);
        b.emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        b.emptyView.setText(query.isEmpty()
                ? "No customers yet. Tap + Customer to add the first one."
                : "No customer matches \"" + query + "\".");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
