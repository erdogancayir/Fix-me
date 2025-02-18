package com.avaj;

import com.avaj.trading.market.Market;
public class Main {
    public static void main(String[] args) {
        Market market = new Market();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            market.shutdown();
            System.out.println("Application shutting down...");
        }));

        market.start();
    }
}