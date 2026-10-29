package com.ledger.customerledger.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.databinding.FragmentHomeBinding;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.model.Totals;
import com.ledger.customerledger.reminder.NotificationHelper;
import com.ledger.customerledger.ui.adapter.ChequeAdapter;
import com.ledger.customerledger.ui.adapter.CustomerAdapter;
import com.ledger.customerledger.ui.cheque.ChequeActions;
import com.ledger.customerledger.ui.customer.CustomerDetailActivity;
import com.ledger.customerledger.ui.dialog.ConfirmDialog;
import com.ledger.customerledger.util.BackupFiles;
import com.ledger.customerledger.util.Dates;
import com.ledger.customerledger.util.FileExport;
import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/** Totals, cheques to chase, and the customers who owe most. */
public class HomeFragment extends RefreshableFragment {

    private FragmentHomeBinding b;
    private ChequeAdapter dueAdapter;
    private CustomerAdapter topAdapter;

    /** Opens the phone's file picker; the chosen backup file comes back here. */
    private final ActivityResultLauncher<Intent> pickBackup = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri != null) restore(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentHomeBinding.inflate(inflater, container, false);

        dueAdapter = new ChequeAdapter(true, new ChequeActions(requireContext(), this::onDataChanged));
        b.dueList.setAdapter(dueAdapter);

        topAdapter = new CustomerAdapter(false, new CustomerAdapter.Listener() {
            @Override
            public void onOpen(CustomerAccount a) {
                startActivity(CustomerDetailActivity.intent(requireContext(), a.customer.id));
            }

            @Override
            public void onDelete(CustomerAccount a) {}
        });
        b.topList.setAdapter(topAdapter);

        b.btnTestNotification.setOnClickListener(v -> {
            NotificationHelper.showDueCheques(requireContext(), true);
            Toast.makeText(requireContext(), "Sent. Pull down the status bar to see it", Toast.LENGTH_SHORT).show();
        });
        b.btnExportBalances.setOnClickListener(v -> {
            boolean ok = FileExport.saveToDownloads(requireContext(), "balances-" + Dates.today() + ".csv",
                    "text/csv", LedgerRepository.get(requireContext()).exportBalancesCsv());
            Toast.makeText(requireContext(), ok ? "Saved to Downloads" : "Could not save the file",
                    Toast.LENGTH_SHORT).show();
        });
        b.btnBackup.setOnClickListener(v -> backup());
        b.btnRestore.setOnClickListener(v -> ConfirmDialog.show(requireContext(), "Restore from backup",
                "This replaces ALL customers, bills, payments and cheques on this phone with the data in the "
                        + "backup file. Anything entered after that backup is lost.",
                "Choose file", () -> {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("*/*");
                    pickBackup.launch(pick);
                }));
        return b.getRoot();
    }

    private void backup() {
        final Uri uri = BackupFiles.save(requireContext());
        if (b != null) b.lastBackup.setText(BackupFiles.lastBackupText(requireContext()));
        if (uri == null) {
            Toast.makeText(requireContext(), "Could not save the backup", Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Backup saved")
                .setMessage("Saved in your Downloads folder. Now send a copy to Google Drive or WhatsApp so it "
                        + "survives if the phone is lost.")
                .setPositiveButton("Send copy", (d, w) -> BackupFiles.share(requireContext(), uri))
                .setNegativeButton("Done", null)
                .show();
    }

    private void restore(Uri uri) {
        try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new java.io.IOException("Could not open the file.");
            LedgerRepository.get(requireContext()).restoreFrom(in);
            Toast.makeText(requireContext(), "Restored", Toast.LENGTH_LONG).show();
            onDataChanged();
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage() == null ? "Restore failed" : e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void refresh() {
        if (b == null) return;
        LedgerRepository repo = LedgerRepository.get(requireContext());

        b.lastBackup.setText(BackupFiles.lastBackupText(requireContext()));
        Totals t = repo.getTotals();
        b.totalToCollect.setText(Money.format(t.toCollect));
        b.salesValue.setText(Money.format(t.sales));
        b.receivedValue.setText(Money.format(t.received));
        b.inHandValue.setText(Money.format(t.chequesInHand));
        b.awaitedValue.setText(Money.format(t.chequesAwaited));

        List<ChequeRow> due = repo.getDueSoon();
        b.dueSection.setVisibility(due.isEmpty() ? View.GONE : View.VISIBLE);
        b.dueBanner.setText(due.size() + (due.size() == 1 ? " cheque is" : " cheques are")
                + " due within 2 days or overdue.");
        dueAdapter.submit(due);

        List<CustomerAccount> top = new ArrayList<>();
        for (CustomerAccount a : repo.getAccounts(null)) {
            if (a.getBalance() > 0 && top.size() < 5) top.add(a);
        }
        topAdapter.submit(top);

        boolean noCustomers = t.customers == 0;
        b.topLabel.setVisibility(top.isEmpty() ? View.GONE : View.VISIBLE);
        b.emptyView.setVisibility(top.isEmpty() ? View.VISIBLE : View.GONE);
        b.emptyView.setText(noCustomers
                ? "No customers yet. Open the Customers tab and tap + Customer."
                : "No customer owes anything right now.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
