package com.ledger.customerledger.model;

/** One line in a customer's account history. */
public class LedgerEntry {
    public enum Kind { PURCHASE, PAYMENT, CHEQUE }

    public final Kind kind;
    public final long id;
    public String date = "";
    public String title = "";
    public String subtitle = "";
    public double amount;          // positive adds to balance, negative reduces it
    public double runningBalance;

    public LedgerEntry(Kind kind, long id) {
        this.kind = kind;
        this.id = id;
    }
}
