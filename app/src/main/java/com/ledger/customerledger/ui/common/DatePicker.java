package com.ledger.customerledger.ui.common;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import com.ledger.customerledger.util.Dates;

import java.time.LocalDate;

/** Makes a text field open a calendar when tapped. The value is stored as yyyy-MM-dd. */
public final class DatePicker {
    private DatePicker() {}

    public static void attach(final Context ctx, final EditText field, String initialIso) {
        field.setText(initialIso == null ? "" : initialIso);
        field.setOnClickListener(v -> {
            String text = field.getText().toString();
            LocalDate d = Dates.isValid(text) ? LocalDate.parse(text) : LocalDate.now();
            new DatePickerDialog(ctx,
                    (picker, year, month, day) -> field.setText(LocalDate.of(year, month + 1, day).toString()),
                    d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
    }
}
