package com.avaj.trading.router;

public class ChecksumValidator {
    public static boolean isValid(String message) {
        String[] parts = message.split("\\|");
        if (parts.length != 7)
            return false;

        try {
            return parts.length == 7; // basit doğrulama mantığı
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
