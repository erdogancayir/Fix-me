package com.avaj.trading.market;

import java.io.IOException;

public class Market {
    private String marketId;
    private MarketSocketManager socketManager;

    public Market() {
        this.socketManager = new MarketSocketManager(this);
    }

    public void start() {
        try {
            socketManager.startConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public void processOrder(String message) {
        if (!ChecksumValidator.isValid(message)) {
            System.out.println("Invalid checksum! Message rejected.");
            return;
        }

        String[] parts = message.split("\\|");
        String brokerId = parts[0];
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
        String responseMessage = brokerId + "|" + status + "|" + instrument + "|" + quantity + "|" + market + "|" + price + "|" + generateChecksum();

        socketManager.sendMessage(responseMessage);
    }

    private String generateChecksum() {
        return String.valueOf((int) (Math.random() * 10000));
    }

    public void shutdown() {
        socketManager.shutdown();
        System.out.println("Market shut down complete.");
    }
}
