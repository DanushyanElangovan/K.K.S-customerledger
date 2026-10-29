package com.ledger.customerledger.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.ledger.customerledger.util.ItemCatalog;

import java.util.ArrayList;
import java.util.List;

/** Drop-down list under the Item box on a bill: shows products that match what is typed. */
public class ItemSuggestionAdapter extends BaseAdapter implements Filterable {
    private static final int MAX_SHOWN = 30;

    private final Context ctx;
    private final List<String> all;
    private List<String> shown = new ArrayList<>();

    public ItemSuggestionAdapter(Context ctx, List<String> all) {
        this.ctx = ctx;
        this.all = all;
    }

    @Override public int getCount() { return shown.size(); }
    @Override public String getItem(int position) { return shown.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView row = (TextView) convertView;
        if (row == null) {
            row = (TextView) LayoutInflater.from(ctx)
                    .inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        row.setText(shown.get(position));
        return row;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<String> found = ItemCatalog.search(all, constraint == null ? null : constraint.toString(), MAX_SHOWN);
                FilterResults results = new FilterResults();
                results.values = found;
                results.count = found.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                shown = results.values == null ? new ArrayList<String>() : (List<String>) results.values;
                if (shown.isEmpty()) notifyDataSetInvalidated(); else notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return String.valueOf(resultValue);
            }
        };
    }
}