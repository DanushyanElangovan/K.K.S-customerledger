package com.ledger.customerledger.util;

import com.ledger.customerledger.AppConfig;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}

    /** e.g. "Rs. 12,500" or "Rs. 12,500.50". */
    public static String format(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(Math.abs(value - Math.rint(value)) < 0.005 ? 0 : 2);
        return AppConfig.CURRENCY_PREFIX + nf.format(value);
    }

    /** Parses text typed by the user; returns 0 when it is not a number. */
    public static double parse(String text) {
        try {
            return round(Double.parseDouble(text.trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Parses a quantity such as 0.125 without rounding to 2 decimals. */
    public static double number(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Plain number for edit fields: 1500 instead of 1500.0. */
    public static String plain(double value) {
        return Math.abs(value - Math.rint(value)) < 0.005 ? String.valueOf((long) Math.rint(value)) : String.valueOf(value);
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}