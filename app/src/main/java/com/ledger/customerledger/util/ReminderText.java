package com.ledger.customerledger.util;

import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeRow;

/** The message you send to a customer about a cheque. Edit the wording here. */
public final class ReminderText {
    private ReminderText() {}

    public static String build(ChequeRow row) {
        Cheque c = row.cheque;
        String when;
        if (c.dueDate == null || c.dueDate.isEmpty()) {
            when = "is due soon";
        } else {
            boolean past = Dates.daysUntil(c.dueDate) < 0;
            when = (past ? "was due on " : "is due on ") + Dates.pretty(c.dueDate);
        }
        if (c.received) {
            return "Dear " + row.customerName + ", a friendly reminder that your cheque"
                    + (c.number.isEmpty() ? "" : " no. " + c.number)
                    + (c.bank.isEmpty() ? "" : " (" + c.bank + ")")
                    + " for " + Money.format(c.amount) + " " + when
                    + ". Please make sure the account has enough funds. Thank you.";
        }
        return "Dear " + row.customerName + ", a friendly reminder that the cheque for "
                + (c.bill.isEmpty() ? "your account" : "bill " + c.bill)
                + " (" + Money.format(c.amount) + ") " + when
                + ". Please send it to us on time. Thank you.";
    }
}
