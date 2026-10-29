package com.ledger.customerledger.model;

import com.ledger.customerledger.util.Money;

import java.util.ArrayList;
import java.util.List;

/** Everything needed to draw one bill on paper. Built by BillExport, drawn by BillPdf. */
public class BillDocument {
    public String companyName = "";
    public String companyAddress = "";
    public String companyContact = "";
    public String copyright = "";

    public String billNo = "";
    public String date = "";          // already formatted for display
    public String customerName = "";
    public String customerPhone = "";

    public List<BillItem> items = new ArrayList<>();
    public double total;
    public double paidNow;

    public double balanceDue() {
        return Money.round(total - paidNow);
    }

    /** e.g. Bill-0005-Kumar.pdf */
    public String fileName() {
        String name = customerName.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        String no = billNo.replaceAll("[^A-Za-z0-9]+", "_");
        return "Bill-" + no + (name.isEmpty() ? "" : "-" + name) + ".pdf";
    }
}