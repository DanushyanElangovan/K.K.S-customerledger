package com.ledger.customerledger.model;

/** A bill: goods sold to a customer on credit. Adds to what the customer owes. */
public class Purchase {
    public long id;
    public long customerId;
    public String date = "";   // yyyy-MM-dd
    public String bill = "";
    public double amount;
    public String note = "";
}
