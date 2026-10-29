package com.ledger.customerledger.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.provider.MediaStore;

import com.ledger.customerledger.R;
import com.ledger.customerledger.data.LedgerRepository;
import com.ledger.customerledger.model.BillDocument;
import com.ledger.customerledger.model.BillItem;
import com.ledger.customerledger.model.CustomerAccount;
import com.ledger.customerledger.model.LedgerEntry;
import com.ledger.customerledger.model.Purchase;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/** Turns a saved bill into a PDF: save to Downloads, share, or print. */
public final class BillExport {
    private BillExport() {}

    public static BillDocument create(Context ctx, Purchase bill, List<BillItem> items) {
        LedgerRepository repo = LedgerRepository.get(ctx);
        BillDocument d = new BillDocument();
        d.companyName = ctx.getString(R.string.company_name);
        d.companyAddress = ctx.getString(R.string.company_address);
        d.companyContact = ctx.getString(R.string.company_contact);
        d.copyright = ctx.getString(R.string.company_copyright);
        d.billNo = bill.bill;
        d.date = Dates.pretty(bill.date);
        d.items = items;
        d.total = bill.amount;

        CustomerAccount account = repo.getAccount(bill.customerId);
        if (account != null) {
            d.customerName = account.customer.name;
            d.customerPhone = account.customer.phone;
        }
        // Money paid at the moment of billing is stored as a payment with this note.
        String marker = "Paid with bill " + bill.bill;
        for (LedgerEntry e : repo.getHistory(bill.customerId)) {
            if (e.kind == LedgerEntry.Kind.PAYMENT && marker.equals(e.subtitle)) {
                d.paidNow += -e.amount;
            }
        }
        d.paidNow = Money.round(d.paidNow);
        return d;
    }

    /** Saves the PDF into Downloads. Returns its Uri, or null if it failed. */
    public static Uri savePdf(Context ctx, BillDocument doc) {
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, doc.fileName());
            v.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return null;
            try (OutputStream out = ctx.getContentResolver().openOutputStream(uri)) {
                if (out == null) return null;
                BillPdf.write(doc, out);
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    /** Opens the share sheet (WhatsApp, email, Bluetooth, Drive...) for a saved PDF. */
    public static void share(Context ctx, Uri pdf, String billNo) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, pdf);
        send.putExtra(Intent.EXTRA_SUBJECT, "Bill " + billNo + " - " + ctx.getString(R.string.company_name));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, "Share bill");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(chooser);
    }

    /** Sends the bill to Android's print system (Wi-Fi printers, or Bluetooth through a print-service app). */
    public static void print(Context ctx, final BillDocument doc) {
        PrintManager pm = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        if (pm == null) return;
        String jobName = "Bill " + doc.billNo;
        pm.print(jobName, new BillPrintAdapter(doc), new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
    }

    private static final class BillPrintAdapter extends PrintDocumentAdapter {
        private final BillDocument doc;

        BillPrintAdapter(BillDocument doc) { this.doc = doc; }

        @Override
        public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs, CancellationSignal cancel,
                             LayoutResultCallback callback, Bundle extras) {
            if (cancel.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(doc.fileName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancel,
                            WriteResultCallback callback) {
            try (OutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
                BillPdf.write(doc, out);
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (Exception e) {
                callback.onWriteFailed(e.getMessage());
            }
        }
    }
}