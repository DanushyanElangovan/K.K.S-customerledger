package com.ledger.customerledger.model;

import com.ledger.customerledger.util.Money;

/** A customer plus the figures calculated from their purchases, payments and cheques. */
public class CustomerAccount {
    public Customer customer = new Customer();
    public double purchased;      // total of all bills
    public double paidCash;       // payments received
    public double clearedCheques; // cheques that have cleared
    public double inHand;         // cheques received, not cleared yet
    public double awaited;        // cheques expected, not received yet
    public int pendingCheques;

    public double getPaid() { return Money.round(paidCash + clearedCheques); }

    /** Balance to be paid. Negative means the customer has credit. */
    public double getBalance() { return Money.round(purchased - getPaid()); }

    /** What would remain once all cheques in hand clear. */
    public double getBalanceAfterCheques() { return Money.round(getBalance() - inHand); }
}
