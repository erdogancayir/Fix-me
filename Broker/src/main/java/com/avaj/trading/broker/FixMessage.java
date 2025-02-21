package com.avaj.trading.broker;

public class FixMessage {
    private final String id;
    private final OrderType orderType;
    private final String instrument;
    private final int quantity;
    private final String market;
    private final double price;

    public FixMessage(String id, OrderType orderType, String instrument, int quantity, String market, double price) {
        this.id = id;
        this.orderType = orderType;
        this.instrument = instrument;
        this.quantity = quantity;
        this.market = market;
        this.price = price;
    }

    public String generateChecksum(String message) {
        int checksum = message.chars().sum() % 10000;
        return String.valueOf(checksum);
    }

    public String toFixString() {
        String message = id + "|" + orderType + "|" + instrument + "|" + quantity + "|" + market + "|" + price;
        String checksum = generateChecksum(message);

        return message + "|" + checksum;
    }
}
