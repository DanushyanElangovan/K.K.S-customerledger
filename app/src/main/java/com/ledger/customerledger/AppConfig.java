package com.ledger.customerledger;

/** Settings you are most likely to change. */
public final class AppConfig {
    /** Shown before every amount. */
    public static final String CURRENCY_PREFIX = "Rs. ";

    /** Cheques due within this many days (or overdue) trigger reminders. */
    public static final int REMIND_DAYS_BEFORE = 2;

    /** Hour of the day (0-23) for the daily reminder notification. */
    public static final int REMINDER_HOUR = 9;

    /** Country code used to turn 077... into 9477... for WhatsApp. */
    public static final String COUNTRY_CODE = "94";

    private AppConfig() {}
}
