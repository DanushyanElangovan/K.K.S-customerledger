package com.ledger.customerledger.model;

/** Money received from a customer (cash, bank transfer...). Reduces what they owe. */
public class Payment {
    public long id;
    public long customerId;
    public String date = "";   // yyyy-MM-dd
    public double amount;
    public String method = "Cash";
    public String note = "";
}
