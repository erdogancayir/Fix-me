package com.avaj.trading.broker;

import java.util.Random;

public class FixMessage {
    private String id;
    private OrderType orderType;
    private String instrument;
    private int quantity;
    private String market;
    private double price;

    public FixMessage(String id, OrderType orderType, String instrument, int quantity, String market, double price) {
        this.id = id;
        this.orderType = orderType;
        this.instrument = instrument;
        this.quantity = quantity;
        this.market = market;
        this.price = price;
    }

    // Checksum üreten basit bir algoritma (Rastgele örnek)
    private String generateChecksum() {
        Random rand = new Random();
        return String.format("%04d", rand.nextInt(10000));
    }

    public String toFixString() {
        return id + "|" + orderType + "|" + instrument + "|" + quantity + "|" + market + "|" + price + "|" + generateChecksum();
    }
}
