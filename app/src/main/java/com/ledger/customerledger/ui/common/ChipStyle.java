package com.ledger.customerledger.ui.common;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ledger.customerledger.R;

/** Colours the small rounded labels (Due today, Cleared, ...). */
public final class ChipStyle {
    public enum Tone { NEUTRAL, WARN, BAD, GOOD }

    private ChipStyle() {}

    public static void apply(TextView view, String text, Tone tone) {
        int background;
        int color;
        switch (tone) {
            case WARN:
                background = R.drawable.bg_chip_warn;
                color = R.color.warn;
                break;
            case BAD:
                background = R.drawable.bg_chip_bad;
                color = R.color.bad;
                break;
            case GOOD:
                background = R.drawable.bg_chip_good;
                color = R.color.good;
                break;
            default:
                background = R.drawable.bg_chip_neutral;
                color = R.color.muted;
        }
        view.setText(text);
        view.setBackgroundResource(background);
        view.setTextColor(ContextCompat.getColor(view.getContext(), color));
    }
}
