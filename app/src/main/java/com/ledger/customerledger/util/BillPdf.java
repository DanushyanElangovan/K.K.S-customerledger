package com.ledger.customerledger.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.ledger.customerledger.model.BillDocument;
import com.ledger.customerledger.model.BillItem;

import java.io.IOException;
import java.io.OutputStream;

/** Draws a bill as an A4 PDF. Change the layout here. */
public final class BillPdf {
    private BillPdf() {}

    private static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 36;
    private static final int RIGHT = PAGE_W - MARGIN;           // 559
    private static final int COL_NO = MARGIN, COL_ITEM = 60;
    private static final int QTY_RIGHT = 350, PRICE_RIGHT = 450, AMOUNT_RIGHT = RIGHT;
    private static final int ITEM_WIDTH = 240;
    private static final int FOOTER_Y = PAGE_H - 24;
    private static final int CONTENT_BOTTOM = PAGE_H - 50;
    private static final int GREEN = Color.rgb(15, 92, 77);
    private static final int GREY = Color.rgb(90, 106, 101);

    private static class Ctx {
        PdfDocument doc;
        PdfDocument.Page page;
        Canvas c;
        int pageNo = 0;
        float y;
    }

    public static void write(BillDocument bill, OutputStream out) throws IOException {
        Ctx x = new Ctx();
        x.doc = new PdfDocument();
        try {
            newPage(x, bill, true);

            TextPaint rowPaint = text(10, false, Color.BLACK);
            int n = 0;
            for (BillItem it : bill.items) {
                n++;
                StaticLayout nameLayout = new StaticLayout(
                        it.name, rowPaint, ITEM_WIDTH, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
                int lines = Math.min(nameLayout.getLineCount(), 3);
                float rowH = Math.max(18, lines * 13 + 6);
                if (x.y + rowH > CONTENT_BOTTOM) newPage(x, bill, false);

                float top = x.y;
                Paint p = text(10, false, Color.BLACK);
                x.c.drawText(String.valueOf(n), COL_NO, top + 12, p);
                x.c.save();
                x.c.translate(COL_ITEM, top + 2);
                x.c.clipRect(0, 0, ITEM_WIDTH, lines * 13 + 2);
                nameLayout.draw(x.c);
                x.c.restore();
                right(x.c, Money.plain(it.qty), QTY_RIGHT, top + 12, p);
                right(x.c, amount(it.price), PRICE_RIGHT, top + 12, p);
                right(x.c, amount(it.total()), AMOUNT_RIGHT, top + 12, p);
                x.y += rowH;
                line(x.c, MARGIN, RIGHT, x.y, Color.rgb(225, 232, 229));
            }

            // Totals need about 90pt.
            if (x.y + 100 > CONTENT_BOTTOM) newPage(x, bill, false);
            x.y += 12;
            Paint label = text(11, false, Color.BLACK);
            Paint bold = text(12, true, Color.BLACK);
            right(x.c, "Total", PRICE_RIGHT, x.y + 12, label);
            right(x.c, Money.format(bill.total), AMOUNT_RIGHT, x.y + 12, bold);
            x.y += 20;
            if (bill.paidNow > 0) {
                right(x.c, "Paid now", PRICE_RIGHT, x.y + 12, label);
                right(x.c, Money.format(bill.paidNow), AMOUNT_RIGHT, x.y + 12, label);
                x.y += 20;
            }
            line(x.c, PRICE_RIGHT - 80, RIGHT, x.y, Color.BLACK);
            x.y += 4;
            Paint due = text(13, true, GREEN);
            right(x.c, "Balance due", PRICE_RIGHT, x.y + 14, due);
            right(x.c, Money.format(bill.balanceDue()), AMOUNT_RIGHT, x.y + 14, due);
            x.y += 40;
            x.c.drawText("Thank you for your business.", MARGIN, x.y, text(11, false, GREY));

            finishPage(x, bill);
            x.doc.writeTo(out);
        } finally {
            x.doc.close();
        }
    }

    private static void newPage(Ctx x, BillDocument b, boolean first) {
        if (x.page != null) finishPage(x, b);
        x.pageNo++;
        x.page = x.doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, x.pageNo).create());
        x.c = x.page.getCanvas();
        float y = MARGIN;

        if (first) {
            x.c.drawText(b.companyName, MARGIN, y + 18, text(20, true, GREEN));
            x.c.drawText(b.companyAddress, MARGIN, y + 34, text(10, false, GREY));
            x.c.drawText(b.companyContact, MARGIN, y + 48, text(10, false, GREY));
            right(x.c, "INVOICE", RIGHT, y + 18, text(18, true, Color.BLACK));
            right(x.c, "Bill No: " + b.billNo, RIGHT, y + 36, text(10, false, Color.BLACK));
            right(x.c, "Date: " + b.date, RIGHT, y + 50, text(10, false, Color.BLACK));
            y += 62;
            line(x.c, MARGIN, RIGHT, y, GREEN);
            y += 16;
            x.c.drawText("BILL TO", MARGIN, y, text(9, true, GREY));
            y += 15;
            x.c.drawText(b.customerName, MARGIN, y, text(13, true, Color.BLACK));
            if (!b.customerPhone.isEmpty()) {
                y += 14;
                x.c.drawText(b.customerPhone, MARGIN, y, text(10, false, Color.BLACK));
            }
            y += 22;
        } else {
            x.c.drawText(b.companyName + " – Bill " + b.billNo + " (continued)", MARGIN, y + 12,
                    text(11, true, GREEN));
            y += 28;
        }

        // Table header
        Paint h = text(10, true, Color.WHITE);
        Paint fill = new Paint();
        fill.setColor(GREEN);
        x.c.drawRect(MARGIN, y, RIGHT, y + 20, fill);
        x.c.drawText("#", COL_NO + 4, y + 14, h);
        x.c.drawText("Item", COL_ITEM, y + 14, h);
        right(x.c, "Qty", QTY_RIGHT, y + 14, h);
        right(x.c, "Price", PRICE_RIGHT, y + 14, h);
        right(x.c, "Amount", AMOUNT_RIGHT - 4, y + 14, h);
        x.y = y + 20;
    }

    private static void finishPage(Ctx x, BillDocument b) {
        line(x.c, MARGIN, RIGHT, FOOTER_Y - 14, Color.rgb(200, 210, 206));
        Paint f = text(9, false, GREY);
        f.setTextAlign(Paint.Align.CENTER);
        x.c.drawText(b.copyright, PAGE_W / 2f, FOOTER_Y, f);
        right(x.c, "Page " + x.pageNo, RIGHT, FOOTER_Y, text(9, false, GREY));
        x.doc.finishPage(x.page);
        x.page = null;
    }

    /** 12,500.00 style number without the currency prefix. */
    private static String amount(double v) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    private static TextPaint text(float size, boolean bold, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }

    private static void right(Canvas c, String s, float xRight, float y, Paint p) {
        float w = p.measureText(s);
        c.drawText(s, xRight - w, y, p);
    }

    private static void line(Canvas c, float x1, float x2, float y, int color) {
        Paint p = new Paint();
        p.setColor(color);
        p.setStrokeWidth(0.8f);
        c.drawLine(x1, y, x2, y, p);
    }
}