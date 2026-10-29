package com.ledger.customerledger.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.FragmentChequesBinding;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeFilter;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.ui.adapter.ChequeAdapter;
import com.ledger.customerledger.ui.cheque.ChequeActions;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.FileExport;
import com.ledger.customerledger.util.Money;

import java.util.List;

/** All cheques, filtered by In hand / Not received / Cleared / Bounced. */
public class ChequesFragment extends RefreshableFragment {

    private FragmentChequesBinding b;
    private ChequeAdapter adapter;
    private ChequeFilter filter = ChequeFilter.IN_HAND;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentChequesBinding.inflate(inflater, container, false);

        adapter = new ChequeAdapter(true, new ChequeActions(requireContext(), this::onDataChanged));
        b.chequeList.setAdapter(adapter);

        b.filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_not_received) filter = ChequeFilter.NOT_RECEIVED;
            else if (id == R.id.chip_cleared) filter = ChequeFilter.CLEARED;
            else if (id == R.id.chip_bounced) filter = ChequeFilter.BOUNCED;
            else filter = ChequeFilter.IN_HAND;
            refresh();
        });

        b.btnExport.setOnClickListener(v -> {
            boolean ok = FileExport.saveToDownloads(requireContext(), "cheques-" + Dates.today() + ".csv",
                    "text/csv", LedgerRepository.get(requireContext()).exportChequesCsv());
            Toast.makeText(requireContext(), ok ? "Saved to Downloads" : "Could not save the file",
                    Toast.LENGTH_SHORT).show();
        });
        return b.getRoot();
    }

    @Override
    protected void refresh() {
        if (b == null) return;
        LedgerRepository repo = LedgerRepository.get(requireContext());

        b.chipInHand.setText("In hand (" + repo.countCheques(ChequeFilter.IN_HAND) + ")");
        b.chipNotReceived.setText("Not received (" + repo.countCheques(ChequeFilter.NOT_RECEIVED) + ")");
        b.chipCleared.setText("Cleared (" + repo.countCheques(ChequeFilter.CLEARED) + ")");
        b.chipBounced.setText("Bounced (" + repo.countCheques(ChequeFilter.BOUNCED) + ")");

        List<ChequeRow> rows = repo.getCheques(filter);
        adapter.submit(rows);
        b.emptyView.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);

        double total = 0;
        for (ChequeRow r : rows) total += r.cheque.amount;
        b.summary.setText(rows.isEmpty() ? "" : rows.size() + (rows.size() == 1 ? " cheque" : " cheques")
                + " · " + Money.format(total));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
