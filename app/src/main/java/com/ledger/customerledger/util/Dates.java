package com.ledger.customerledger.util;

import com.ledger.customerledger.AppConfig;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Dates are stored as text in the form yyyy-MM-dd. */
public final class Dates {
    private Dates() {}

    public enum Level { NONE, WARN, BAD }

    public static final class DueInfo {
        public final String text;
        public final Level level;

        DueInfo(String text, Level level) {
            this.text = text;
            this.level = level;
        }
    }

    private static final DateTimeFormatter PRETTY =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public static String today() {
        return LocalDate.now().toString();
    }

    /** Latest due date that counts as "due soon" (today + REMIND_DAYS_BEFORE). */
    public static String dueSoonLimit() {
        return LocalDate.now().plusDays(AppConfig.REMIND_DAYS_BEFORE).toString();
    }

    /** Days from today until the date. Negative when the date has passed. */
    public static long daysUntil(String iso) {
        return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(iso));
    }

    public static boolean isValid(String iso) {
        try {
            LocalDate.parse(iso);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String pretty(String iso) {
        if (iso == null || iso.isEmpty()) return "No date";
        try {
            return LocalDate.parse(iso).format(PRETTY);
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    public static DueInfo dueInfo(String iso) {
        if (iso == null || iso.isEmpty() || !isValid(iso)) return new DueInfo("No date set", Level.NONE);
        long d = daysUntil(iso);
        if (d < 0) return new DueInfo("Overdue " + (-d) + (d == -1 ? " day" : " days"), Level.BAD);
        if (d == 0) return new DueInfo("Due today", Level.WARN);
        if (d == 1) return new DueInfo("Due tomorrow", Level.WARN);
        Level level = d <= AppConfig.REMIND_DAYS_BEFORE ? Level.WARN : Level.NONE;
        return new DueInfo("Due in " + d + " days", level);
    }
}
