package com.avaj.trading.broker;

public class FixMessage {
    public final String id;
    public final OrderType orderType;
    public final String instrument;
    public final int quantity;
    public final String market;
    public final double price;

    public String checksum;

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
        this.checksum = String.valueOf(checksum);

        return this.checksum;
    }

    public String toFixString() {
        String message = id + "|" + orderType + "|" + instrument + "|" + quantity + "|" + market + "|" + price;
        String checksum = generateChecksum(message);

        return message + "|" + checksum;
    }
}
