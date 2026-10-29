package com.ledger.customerledger.ui.common;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputLayout;
import com.ledger.customerledger.util.Money;

/** Small helpers shared by all the forms. */
public final class Forms {
    private Forms() {}

    public static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Shows "Required" under the field when it is empty. */
    public static boolean requireText(TextInputLayout layout, EditText field) {
        if (text(field).isEmpty()) {
            layout.setError("Required");
            return false;
        }
        layout.setError(null);
        return true;
    }

    /** Shows an error unless the field holds a number greater than zero. */
    public static boolean requireAmount(TextInputLayout layout, EditText field) {
        if (Money.parse(text(field)) <= 0) {
            layout.setError("Enter an amount");
            return false;
        }
        layout.setError(null);
        return true;
    }

    public static void setupSpinner(Context ctx, Spinner spinner, String[] items, int selected) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
    }
}
