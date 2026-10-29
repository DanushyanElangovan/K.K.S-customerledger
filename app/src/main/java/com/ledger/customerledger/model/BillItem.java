package com.ledger.customerledger.model;

import com.ledger.customerledger.util.Money;

/** One line on a bill: an item, how many, and the price of one. */
public class BillItem {
    public long id;
    public long purchaseId;
    public String name = "";
    public double qty = 1;
    public double price;

    public BillItem() {}

    public BillItem(String name, double qty, double price) {
        this.name = name;
        this.qty = qty;
        this.price = price;
    }

    public double total() {
        return Money.round(qty * price);
    }
}