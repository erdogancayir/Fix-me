package com.avaj.trading.market;

import java.io.IOException;

public class Market {
    private String marketId;
    private final MarketSocketManager socketManager;

    public Market() {
        this.socketManager = new MarketSocketManager(this);
    }

    public void start() {
        try {
            socketManager.startConnection();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getMarketId() {
        return this.marketId;
    }

    public boolean processOrder(String message) {
        System.out.println("Received raw order: " + message);

        if (!ChecksumValidator.isValid(message)) {
            System.out.println("Invalid checksum! Message rejected.");
            return false;
        }

        String[] parts = message.split("\\|");
        if (parts.length < 6) { // Expected format: BUY/SELL|Instrument|Quantity|Market|Price|Checksum
            System.err.println("Invalid FIX message format. Expected at least 6 parts, got: " + parts.length);
            return false;
        }

        String orderType = parts[1];
        String instrument = parts[2];
        int quantity = Integer.parseInt(parts[3]);
        String market = parts[4];
        double price = Double.parseDouble(parts[5]);

        boolean isExecuted = false;
        if ("BUY".equals(orderType)) {
            isExecuted = InstrumentDatabase.buyInstrument(instrument, quantity);
        } else if ("SELL".equals(orderType)) {
            isExecuted = InstrumentDatabase.sellInstrument(instrument, quantity);
        }

        String status = isExecuted ? "Executed" : "Rejected";
        String responseMessage = marketId + "|" + status + "|" + instrument + "|" + quantity + "|" + market + "|" + price;
        String checksum = generateChecksum(responseMessage);
        String finalMessage = responseMessage + "|" + checksum;

        socketManager.sendMessage(finalMessage);

        return true;
    }

    public String generateChecksum(String message) {
        int checksum = message.chars().sum() % 10000;
        return String.valueOf(checksum);
    }

    public void shutdown() {
        socketManager.shutdown();
        System.out.println("Market shut down complete.");
    }
}
