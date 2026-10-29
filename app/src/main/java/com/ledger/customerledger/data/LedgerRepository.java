package com.ledger.customerledger.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.ledger.customerledger.model.BillItem;
import com.ledger.customerledger.model.Cheque;
import com.ledger.customerledger.model.ChequeFilter;
import com.ledger.customerledger.model.ChequeRow;
import com.ledger.customerledger.model.Customer;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.model.LedgerEntry;
import com.ledger.customerledger.model.Payment;
import com.ledger.customerledger.model.Purchase;
import com.ledger.customerledger.model.Totals;
import com.ledger.customerledger.util.Csv;
import com.ledger.customerledger.util.Dates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The only class that talks to the database. Screens call these methods and never write SQL.
 * Balance rule: balance = purchases - payments - cleared cheques.
 */
public class LedgerRepository {

    private static LedgerRepository instance;

    public static synchronized LedgerRepository get(Context context) {
        if (instance == null) instance = new LedgerRepository(context.getApplicationContext());
        return instance;
    }

    private final Context appContext;
    private final LedgerDb helper;

    private LedgerRepository(Context appContext) {
        this.appContext = appContext;
        helper = new LedgerDb(appContext);
    }

    private SQLiteDatabase rd() { return helper.getReadableDatabase(); }
    private SQLiteDatabase wr() { return helper.getWritableDatabase(); }

    // ---------------------------------------------------------------- customers

    private static final String ACCOUNT_SQL =
            "SELECT c.id, c.name, c.phone, c.note,"
                    + " (SELECT IFNULL(SUM(amount),0) FROM purchases WHERE customer_id=c.id) AS purchased,"
                    + " (SELECT IFNULL(SUM(amount),0) FROM payments WHERE customer_id=c.id) AS paid_cash,"
                    + " (SELECT IFNULL(SUM(amount),0) FROM cheques WHERE customer_id=c.id AND status='CLEARED') AS cleared,"
                    + " (SELECT IFNULL(SUM(amount),0) FROM cheques WHERE customer_id=c.id AND status='PENDING' AND received=1) AS in_hand,"
                    + " (SELECT IFNULL(SUM(amount),0) FROM cheques WHERE customer_id=c.id AND status='PENDING' AND received=0) AS awaited,"
                    + " (SELECT COUNT(*) FROM cheques WHERE customer_id=c.id AND status='PENDING') AS pending_count"
                    + " FROM customers c";

    private List<CustomerAccount> queryAccounts(String where, String[] args) {
        String sql = ACCOUNT_SQL + (where == null ? "" : " WHERE " + where);
        List<CustomerAccount> list = new ArrayList<>();
        try (Cursor c = rd().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                CustomerAccount a = new CustomerAccount();
                a.customer.id = c.getLong(0);
                a.customer.name = nz(c.getString(1));
                a.customer.phone = nz(c.getString(2));
                a.customer.note = nz(c.getString(3));
                a.purchased = c.getDouble(4);
                a.paidCash = c.getDouble(5);
                a.clearedCheques = c.getDouble(6);
                a.inHand = c.getDouble(7);
                a.awaited = c.getDouble(8);
                a.pendingCheques = c.getInt(9);
                list.add(a);
            }
        }
        return list;
    }

    /** All customers, highest balance first. Pass null or "" for no search. */
    public List<CustomerAccount> getAccounts(String search) {
        List<CustomerAccount> list;
        if (search == null || search.trim().isEmpty()) {
            list = queryAccounts(null, null);
        } else {
            String like = "%" + search.trim() + "%";
            list = queryAccounts("(c.name LIKE ? OR c.phone LIKE ?)", new String[]{like, like});
        }
        Collections.sort(list, new Comparator<CustomerAccount>() {
            @Override
            public int compare(CustomerAccount x, CustomerAccount y) {
                int byBalance = Double.compare(y.getBalance(), x.getBalance());
                return byBalance != 0 ? byBalance : x.customer.name.compareToIgnoreCase(y.customer.name);
            }
        });
        return list;
    }

    public CustomerAccount getAccount(long customerId) {
        List<CustomerAccount> list = queryAccounts("c.id=?", new String[]{String.valueOf(customerId)});
        return list.isEmpty() ? null : list.get(0);
    }

    public long saveCustomer(Customer c) {
        ContentValues v = new ContentValues();
        v.put("name", c.name);
        v.put("phone", c.phone);
        v.put("note", c.note);
        if (c.id == 0) {
            c.id = wr().insert("customers", null, v);
        } else {
            wr().update("customers", v, "id=?", new String[]{String.valueOf(c.id)});
        }
        return c.id;
    }

    /** Also deletes the customer's purchases, payments and cheques. */
    public void deleteCustomer(long id) {
        wr().delete("customers", "id=?", new String[]{String.valueOf(id)});
    }

    public Totals getTotals() {
        Totals t = new Totals();
        for (CustomerAccount a : queryAccounts(null, null)) {
            t.customers++;
            t.toCollect += Math.max(a.getBalance(), 0);
            t.sales += a.purchased;
            t.received += a.getPaid();
            t.chequesInHand += a.inHand;
            t.chequesAwaited += a.awaited;
        }
        return t;
    }

    // ---------------------------------------------------------------- purchases

    public Purchase getPurchase(long id) {
        try (Cursor c = rd().rawQuery(
                "SELECT id, customer_id, date, bill, amount, note FROM purchases WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null;
            Purchase p = new Purchase();
            p.id = c.getLong(0);
            p.customerId = c.getLong(1);
            p.date = nz(c.getString(2));
            p.bill = nz(c.getString(3));
            p.amount = c.getDouble(4);
            p.note = nz(c.getString(5));
            return p;
        }
    }

    public long savePurchase(Purchase p) {
        ContentValues v = new ContentValues();
        v.put("customer_id", p.customerId);
        v.put("date", p.date);
        v.put("bill", p.bill);
        v.put("amount", p.amount);
        v.put("note", p.note);
        if (p.id == 0) {
            p.id = wr().insert("purchases", null, v);
        } else {
            wr().update("purchases", v, "id=?", new String[]{String.valueOf(p.id)});
        }
        return p.id;
    }

    /** Deletes the purchase, its bill items and any cheque that was created for it. */
    public void deletePurchase(long id) {
        String[] args = new String[]{String.valueOf(id)};
        wr().delete("cheques", "purchase_id=?", args);
        wr().delete("purchases", "id=?", args);
    }

    // ---------------------------------------------------------------- payments

    public Payment getPayment(long id) {
        try (Cursor c = rd().rawQuery(
                "SELECT id, customer_id, date, amount, method, note FROM payments WHERE id=?",
                new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null;
            Payment p = new Payment();
            p.id = c.getLong(0);
            p.customerId = c.getLong(1);
            p.date = nz(c.getString(2));
            p.amount = c.getDouble(3);
            p.method = nz(c.getString(4));
            p.note = nz(c.getString(5));
            return p;
        }
    }

    public long savePayment(Payment p) {
        ContentValues v = new ContentValues();
        v.put("customer_id", p.customerId);
        v.put("date", p.date);
        v.put("amount", p.amount);
        v.put("method", p.method);
        v.put("note", p.note);
        if (p.id == 0) {
            p.id = wr().insert("payments", null, v);
        } else {
            wr().update("payments", v, "id=?", new String[]{String.valueOf(p.id)});
        }
        return p.id;
    }

    public void deletePayment(long id) {
        wr().delete("payments", "id=?", new String[]{String.valueOf(id)});
    }

    // ---------------------------------------------------------------- cheques

    private static final String CHEQUE_SQL =
            "SELECT ch.id, ch.customer_id, ch.purchase_id, ch.number, ch.bank, ch.due_date,"
                    + " ch.amount, ch.bill, ch.received, ch.status, ch.cleared_on,"
                    + " c.name, c.phone"
                    + " FROM cheques ch JOIN customers c ON c.id = ch.customer_id";

    private static final String DUE_DATE_FIRST =
            " ORDER BY CASE WHEN ch.due_date IS NULL OR ch.due_date='' THEN 1 ELSE 0 END, ch.due_date ASC";

    private ChequeRow readRow(Cursor c) {
        Cheque ch = new Cheque();
        ch.id = c.getLong(0);
        ch.customerId = c.getLong(1);
        ch.purchaseId = c.getLong(2);
        ch.number = nz(c.getString(3));
        ch.bank = nz(c.getString(4));
        ch.dueDate = nz(c.getString(5));
        ch.amount = c.getDouble(6);
        ch.bill = nz(c.getString(7));
        ch.received = c.getInt(8) == 1;
        ch.status = Cheque.Status.valueOf(c.getString(9));
        ch.clearedOn = nz(c.getString(10));
        ChequeRow row = new ChequeRow();
        row.cheque = ch;
        row.customerName = nz(c.getString(11));
        row.customerPhone = nz(c.getString(12));
        return row;
    }

    private List<ChequeRow> queryCheques(String where, String[] args, String orderBy) {
        String sql = CHEQUE_SQL + (where == null ? "" : " WHERE " + where) + orderBy;
        List<ChequeRow> list = new ArrayList<>();
        try (Cursor c = rd().rawQuery(sql, args)) {
            while (c.moveToNext()) list.add(readRow(c));
        }
        return list;
    }

    private static String whereFor(ChequeFilter f) {
        switch (f) {
            case IN_HAND:      return "ch.status='PENDING' AND ch.received=1";
            case NOT_RECEIVED: return "ch.status='PENDING' AND ch.received=0";
            case CLEARED:      return "ch.status='CLEARED'";
            default:           return "ch.status='BOUNCED'";
        }
    }

    public List<ChequeRow> getCheques(ChequeFilter f) {
        boolean newestFirst = f == ChequeFilter.CLEARED || f == ChequeFilter.BOUNCED;
        String order = newestFirst ? " ORDER BY ch.due_date DESC" : DUE_DATE_FIRST;
        return queryCheques(whereFor(f), null, order);
    }

    public int countCheques(ChequeFilter f) {
        try (Cursor c = rd().rawQuery("SELECT COUNT(*) FROM cheques ch WHERE " + whereFor(f), null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Pending cheques due within AppConfig.REMIND_DAYS_BEFORE days, plus overdue ones. */
    public List<ChequeRow> getDueSoon() {
        return queryCheques(
                "ch.status='PENDING' AND ch.due_date IS NOT NULL AND ch.due_date<>'' AND ch.due_date<=?",
                new String[]{Dates.dueSoonLimit()},
                " ORDER BY ch.due_date ASC");
    }

    /** Cheques of one customer that are not cleared yet (pending or bounced). */
    public List<ChequeRow> getOpenCheques(long customerId) {
        return queryCheques("ch.customer_id=? AND ch.status<>'CLEARED'",
                new String[]{String.valueOf(customerId)}, DUE_DATE_FIRST);
    }

    public Cheque getCheque(long id) {
        List<ChequeRow> rows = queryCheques("ch.id=?", new String[]{String.valueOf(id)}, "");
        return rows.isEmpty() ? null : rows.get(0).cheque;
    }

    public long saveCheque(Cheque ch) {
        ContentValues v = new ContentValues();
        v.put("customer_id", ch.customerId);
        v.put("purchase_id", ch.purchaseId == 0 ? null : ch.purchaseId);
        v.put("number", ch.number);
        v.put("bank", ch.bank);
        v.put("due_date", ch.dueDate);
        v.put("amount", ch.amount);
        v.put("bill", ch.bill);
        v.put("received", ch.received ? 1 : 0);
        v.put("status", ch.status.name());
        v.put("cleared_on", ch.clearedOn);
        if (ch.id == 0) {
            ch.id = wr().insert("cheques", null, v);
        } else {
            wr().update("cheques", v, "id=?", new String[]{String.valueOf(ch.id)});
        }
        return ch.id;
    }

    public void deleteCheque(long id) {
        wr().delete("cheques", "id=?", new String[]{String.valueOf(id)});
    }

    public void markReceived(long id) {
        Cheque ch = getCheque(id);
        if (ch == null) return;
        ch.received = true;
        saveCheque(ch);
    }

    /** Changes the status. CLEARED stamps today's date and reduces the customer's balance. */
    public void setStatus(long id, Cheque.Status status) {
        Cheque ch = getCheque(id);
        if (ch == null) return;
        ch.status = status;
        if (status == Cheque.Status.CLEARED) {
            ch.received = true;
            ch.clearedOn = Dates.today();
        } else {
            ch.clearedOn = "";
        }
        saveCheque(ch);
    }

    // ---------------------------------------------------------------- bills

    /** Suggested next bill number: 0001, 0002, ... */
    public String nextBillNumber() {
        try (Cursor c = rd().rawQuery("SELECT COUNT(*) FROM purchases", null)) {
            int count = c.moveToFirst() ? c.getInt(0) : 0;
            return String.format(Locale.US, "%04d", count + 1);
        }
    }

    public List<BillItem> getBillItems(long purchaseId) {
        List<BillItem> list = new ArrayList<>();
        try (Cursor c = rd().rawQuery(
                "SELECT id, name, qty, price FROM bill_items WHERE purchase_id=? ORDER BY id",
                new String[]{String.valueOf(purchaseId)})) {
            while (c.moveToNext()) {
                BillItem it = new BillItem();
                it.id = c.getLong(0);
                it.purchaseId = purchaseId;
                it.name = nz(c.getString(1));
                it.qty = c.getDouble(2);
                it.price = c.getDouble(3);
                list.add(it);
            }
        }
        return list;
    }

    /**
     * Saves a bill in one go: the purchase (adds to the customer's balance), its items,
     * optional money paid now (stored as a payment) and an optional cheque for the rest.
     * Everything is saved together or not at all.
     */
    public long saveBill(Purchase bill, List<BillItem> items, double paidNow, Cheque cheque) {
        SQLiteDatabase db = wr();
        db.beginTransaction();
        try {
            savePurchase(bill);
            for (BillItem it : items) {
                ContentValues v = new ContentValues();
                v.put("purchase_id", bill.id);
                v.put("name", it.name);
                v.put("qty", it.qty);
                v.put("price", it.price);
                db.insert("bill_items", null, v);
            }
            if (paidNow > 0) {
                Payment pay = new Payment();
                pay.customerId = bill.customerId;
                pay.date = bill.date;
                pay.amount = paidNow;
                pay.method = "Cash";
                pay.note = "Paid with bill " + bill.bill;
                savePayment(pay);
            }
            if (cheque != null) {
                cheque.customerId = bill.customerId;
                cheque.purchaseId = bill.id;
                saveCheque(cheque);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return bill.id;
    }

    // ---------------------------------------------------------------- history

    /** Purchases, payments and cleared cheques of a customer, newest first, with running balance. */
    public List<LedgerEntry> getHistory(long customerId) {
        String id = String.valueOf(customerId);
        List<LedgerEntry> list = new ArrayList<>();

        try (Cursor c = rd().rawQuery(
                "SELECT id, date, bill, amount, note FROM purchases WHERE customer_id=?", new String[]{id})) {
            while (c.moveToNext()) {
                LedgerEntry e = new LedgerEntry(LedgerEntry.Kind.PURCHASE, c.getLong(0));
                e.date = nz(c.getString(1));
                String bill = nz(c.getString(2));
                e.title = "Purchase" + (bill.isEmpty() ? "" : " · Bill " + bill);
                e.subtitle = nz(c.getString(4));
                e.amount = c.getDouble(3);
                list.add(e);
            }
        }
        try (Cursor c = rd().rawQuery(
                "SELECT id, date, amount, method, note FROM payments WHERE customer_id=?", new String[]{id})) {
            while (c.moveToNext()) {
                LedgerEntry e = new LedgerEntry(LedgerEntry.Kind.PAYMENT, c.getLong(0));
                e.date = nz(c.getString(1));
                e.title = "Payment · " + nz(c.getString(3));
                e.subtitle = nz(c.getString(4));
                e.amount = -c.getDouble(2);
                list.add(e);
            }
        }
        try (Cursor c = rd().rawQuery(
                "SELECT id, cleared_on, due_date, amount, number, bank FROM cheques"
                        + " WHERE customer_id=? AND status='CLEARED'", new String[]{id})) {
            while (c.moveToNext()) {
                LedgerEntry e = new LedgerEntry(LedgerEntry.Kind.CHEQUE, c.getLong(0));
                String cleared = nz(c.getString(1));
                e.date = cleared.isEmpty() ? nz(c.getString(2)) : cleared;
                String number = nz(c.getString(4));
                e.title = "Cheque cleared" + (number.isEmpty() ? "" : " · " + number);
                e.subtitle = nz(c.getString(5));
                e.amount = -c.getDouble(3);
                list.add(e);
            }
        }

        Collections.sort(list, new Comparator<LedgerEntry>() {
            @Override
            public int compare(LedgerEntry x, LedgerEntry y) {
                int byDate = x.date.compareTo(y.date);
                if (byDate != 0) return byDate;
                int byKind = x.kind.compareTo(y.kind);
                return byKind != 0 ? byKind : Long.compare(x.id, y.id);
            }
        });
        double running = 0;
        for (LedgerEntry e : list) {
            running += e.amount;
            e.runningBalance = Math.round(running * 100.0) / 100.0;
        }
        Collections.reverse(list);
        return list;
    }

    // ---------------------------------------------------------------- exports

    public String exportChequesCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.row(Arrays.asList("Customer", "Cheque no", "Bank", "Cheque date",
                "Amount", "Bill", "Received", "Status")));
        for (ChequeRow r : queryCheques(null, null, DUE_DATE_FIRST)) {
            Cheque c = r.cheque;
            sb.append(Csv.row(Arrays.asList(r.customerName, c.number, c.bank, c.dueDate,
                    String.valueOf(c.amount), c.bill, c.received ? "Yes" : "No", c.status.name())));
        }
        return sb.toString();
    }

    public String exportBalancesCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.row(Arrays.asList("Customer", "Phone", "Total purchases", "Received",
                "Balance", "Cheques in hand", "Cheques awaited")));
        for (CustomerAccount a : getAccounts(null)) {
            sb.append(Csv.row(Arrays.asList(a.customer.name, a.customer.phone,
                    String.valueOf(a.purchased), String.valueOf(a.getPaid()),
                    String.valueOf(a.getBalance()), String.valueOf(a.inHand), String.valueOf(a.awaited))));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- backup and restore

    private File dbFile() { return appContext.getDatabasePath(LedgerDb.NAME); }

    /** Moves everything still waiting in the write-ahead log into the main database file. */
    private void checkpoint() {
        try (Cursor c = wr().rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
            c.moveToFirst();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        out.flush();
    }

    /** Writes a complete copy of all data (customers, bills, payments, cheques) to the stream. */
    public synchronized void backupTo(OutputStream out) throws IOException {
        checkpoint();
        try (InputStream in = new FileInputStream(dbFile())) {
            copy(in, out);
        }
    }

    /**
     * Replaces ALL current data with the data in a backup file.
     * The file is checked first; if anything fails the current data is put back.
     */
    public synchronized void restoreFrom(InputStream in) throws IOException {
        File tmp = new File(appContext.getCacheDir(), "restore.tmp");
        try (OutputStream o = new FileOutputStream(tmp)) {
            copy(in, o);
        }
        try {
            checkBackupFile(tmp);
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }

        // keep a safety copy of what is there now
        checkpoint();
        File safety = new File(appContext.getFilesDir(), "before-restore.db");
        try (InputStream si = new FileInputStream(dbFile()); OutputStream so = new FileOutputStream(safety)) {
            copy(si, so);
        }

        helper.close();
        File db = dbFile();
        deleteQuietly(db);
        deleteQuietly(new File(db.getPath() + "-wal"));
        deleteQuietly(new File(db.getPath() + "-shm"));
        deleteQuietly(new File(db.getPath() + "-journal"));
        try (InputStream ti = new FileInputStream(tmp); OutputStream to = new FileOutputStream(db)) {
            copy(ti, to);
            wr();   // reopens, and upgrades an older backup if needed
        } catch (IOException | SQLiteException e) {
            helper.close();
            deleteQuietly(db);
            try (InputStream si = new FileInputStream(safety); OutputStream so = new FileOutputStream(db)) {
                copy(si, so);
            }
            wr();
            throw new IOException("Restore failed. Your previous data was kept.");
        } finally {
            tmp.delete();
        }
    }

    private static void deleteQuietly(File f) {
        if (f.exists()) f.delete();
    }

    private static void checkBackupFile(File f) throws IOException {
        SQLiteDatabase t = null;
        try {
            t = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            if (t.getVersion() > LedgerDb.VERSION) {
                throw new IOException("This backup was made by a newer version of the app.");
            }
            for (String table : new String[]{"customers", "purchases", "payments", "cheques"}) {
                try (Cursor c = t.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        new String[]{table})) {
                    if (!c.moveToFirst()) throw new IOException("This is not a backup file from this app.");
                }
            }
        } catch (SQLiteException e) {
            throw new IOException("This is not a valid backup file.");
        } finally {
            if (t != null) t.close();
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}