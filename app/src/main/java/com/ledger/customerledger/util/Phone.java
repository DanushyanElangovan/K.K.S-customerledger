package com.ledger.customerledger.util;

import com.ledger.customerledger.AppConfig;

public final class Phone {
    private Phone() {}

    /** "077 123 4567" becomes "94771234567" for WhatsApp links. */
    public static String forWhatsApp(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.startsWith("0")) digits = AppConfig.COUNTRY_CODE + digits.substring(1);
        return digits;
    }
}
