package com.ledger.customerledger.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The list of products offered while making a bill. It is read from assets/items.txt
 * (one product per line, no prices; lines starting with # are headings and are ignored here).
 * Products that are not in the file can still be typed directly on the bill.
 */
public final class ItemCatalog {
    private static final String FILE = "items.txt";
    private static List<String> cache;

    private ItemCatalog() {}

    /** All product names, loaded once. Returns an empty list if the file is missing. */
    public static synchronized List<String> names(Context ctx) {
        if (cache == null) {
            try (InputStream in = ctx.getApplicationContext().getAssets().open(FILE)) {
                cache = parse(in);
            } catch (IOException e) {
                cache = new ArrayList<>();
            }
        }
        return cache;
    }

    /** Reads one product per line. Blank lines and # headings are skipped. */
    public static List<String> parse(InputStream in) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) list.add(line);
        }
        return list;
    }

    /**
     * Products that contain every word typed, in any order and any capitals.
     * "kettle 1.8" finds "RPCK-1163 KETTLE 1.8L". Returns nothing for an empty query.
     */
    public static List<String> search(List<String> all, String query, int limit) {
        List<String> found = new ArrayList<>();
        if (query == null) return found;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return found;
        String[] words = q.split("\\s+");
        for (String item : all) {
            String lower = item.toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String w : words) {
                if (!lower.contains(w)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                found.add(item);
                if (found.size() >= limit) break;
            }
        }
        return found;
    }
}