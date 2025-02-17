package com.avaj.trading.router;

public class ChecksumValidator {
    public static boolean isValid(String message) {
        String[] parts = message.split("\\|");
        if (parts.length != 7) return false;

        try {
            int checksum = Integer.parseInt(parts[6]);
            return checksum % 2 == 0; // Örnek bir basit doğrulama mantığı
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
