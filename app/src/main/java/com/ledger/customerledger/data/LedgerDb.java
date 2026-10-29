package com.ledger.customerledger.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates the SQLite tables. Dates are text (yyyy-MM-dd).
 *
 * To add a column later: raise VERSION and add an ALTER TABLE in onUpgrade.
 */
public class LedgerDb extends SQLiteOpenHelper {
     static final String NAME = "ledger.db";
     static final int VERSION = 2;

    public LedgerDb(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // deleting a customer deletes their records
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE customers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "phone TEXT,"
                + "note TEXT)");

        db.execSQL("CREATE TABLE purchases ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,"
                + "date TEXT NOT NULL,"
                + "bill TEXT,"
                + "amount REAL NOT NULL,"
                + "note TEXT)");

        db.execSQL("CREATE TABLE payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,"
                + "date TEXT NOT NULL,"
                + "amount REAL NOT NULL,"
                + "method TEXT,"
                + "note TEXT)");

        db.execSQL("CREATE TABLE cheques ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,"
                + "purchase_id INTEGER,"
                + "number TEXT,"
                + "bank TEXT,"
                + "due_date TEXT,"
                + "amount REAL NOT NULL,"
                + "bill TEXT,"
                + "received INTEGER NOT NULL DEFAULT 1,"
                + "status TEXT NOT NULL DEFAULT 'PENDING',"
                + "cleared_on TEXT)");

        db.execSQL("CREATE INDEX idx_purchases_customer ON purchases(customer_id)");
        db.execSQL("CREATE INDEX idx_payments_customer ON payments(customer_id)");
        db.execSQL("CREATE INDEX idx_cheques_customer ON cheques(customer_id)");

        createBillItems(db);
    }

    /** Items of a bill made on the Billing tab. Deleted together with their purchase. */
    private void createBillItems(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS bill_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "purchase_id INTEGER NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,"
                + "name TEXT NOT NULL,"
                + "qty REAL NOT NULL,"
                + "price REAL NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bill_items_purchase ON bill_items(purchase_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createBillItems(db);
        // Next change, for example version 3:
        // if (oldVersion < 3) db.execSQL("ALTER TABLE customers ADD COLUMN address TEXT");
    }
}