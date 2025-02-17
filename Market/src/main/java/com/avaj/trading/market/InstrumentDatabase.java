package com.avaj.trading.market;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class InstrumentDatabase {
    private static final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    static {
        inventory.put("AAPL", 50);
        inventory.put("GOOGL", 30);
        inventory.put("TSLA", 20);
    }

    public static synchronized boolean buyInstrument(String instrument, int quantity) {
        return inventory.getOrDefault(instrument, 0) >= quantity;
    }

    public static synchronized boolean sellInstrument(String instrument, int quantity) {
        inventory.put(instrument, inventory.getOrDefault(instrument, 0) + quantity);
        return true;
    }
}
