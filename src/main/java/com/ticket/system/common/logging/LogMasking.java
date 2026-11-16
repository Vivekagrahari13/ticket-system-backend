package com.ticket.system.common.logging;

// Personal data does not belong in logs. Use these helpers when a value must appear.
public final class LogMasking {

    private LogMasking() {
    }

    // jane.doe@company.com becomes j***@company.com
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return "***";
        }
        if (at == 0) {
            return "***" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
