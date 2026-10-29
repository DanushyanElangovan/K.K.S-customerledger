package com.ledger.customerledger.model;

import com.ledger.customerledger.AppConfig;
import com.ledger.customerledger.util.Dates;

/**
 * A cheque from a customer. It only reduces the balance once its status is CLEARED.
 * received=false means the cheque is expected but not yet in your hands.
 */
public class Cheque {
    public enum Status { PENDING, CLEARED, BOUNCED }

    public long id;
    public long customerId;
    public long purchaseId;        // 0 when not linked to a purchase
    public String number = "";
    public String bank = "";
    public String dueDate = "";    // yyyy-MM-dd, may be empty
    public double amount;
    public String bill = "";
    public boolean received = true;
    public Status status = Status.PENDING;
    public String clearedOn = "";  // yyyy-MM-dd

    /** True when pending and due within AppConfig.REMIND_DAYS_BEFORE days (or already overdue). */
    public boolean isDueSoon() {
        if (status != Status.PENDING || dueDate == null || dueDate.isEmpty()) return false;
        return Dates.daysUntil(dueDate) <= AppConfig.REMIND_DAYS_BEFORE;
    }
}
