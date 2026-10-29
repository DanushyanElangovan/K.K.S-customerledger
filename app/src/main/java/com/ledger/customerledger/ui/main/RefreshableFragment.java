package com.ledger.customerledger.ui.main;

import androidx.fragment.app.Fragment;

/** Base for the three tabs: redraws when shown, and updates the Cheques badge after changes. */
public abstract class RefreshableFragment extends Fragment {

    /** Reload data from the database and redraw. Must tolerate the view being gone. */
    protected abstract void refresh();

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /** Call after anything was added, changed or deleted. */
    protected void onDataChanged() {
        refresh();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshBadge();
    }
}
