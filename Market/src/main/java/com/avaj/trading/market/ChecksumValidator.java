package com.avaj.trading.market;

public class ChecksumValidator {
    public static boolean isValid(String message) {
        String[] parts = message.split("\\|");
        return parts.length == 7 && parts[6].matches("\\d+");
    }
}
