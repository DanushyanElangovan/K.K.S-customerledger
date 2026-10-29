package com.ledger.customerledger.util;

import java.util.List;

public final class Csv {
    private Csv() {}

    public static String row(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(cells.get(i) == null ? "" : cells.get(i).replace("\"", "\"\"")).append('"');
        }
        return sb.append('\n').toString();
    }
}
