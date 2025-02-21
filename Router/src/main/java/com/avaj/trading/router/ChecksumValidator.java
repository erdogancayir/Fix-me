package com.avaj.trading.router;

public class ChecksumValidator {
    public static String generateChecksum(String message) {
        int checksum = message.chars().sum() % 10000;
        return String.valueOf(checksum);
    }

    public static boolean isValid(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 2) return false;

        String receivedChecksum = parts[parts.length - 1];
        String originalMessage = message.substring(0, message.lastIndexOf("|"));

        return receivedChecksum.equals(generateChecksum(originalMessage));
    }
}
