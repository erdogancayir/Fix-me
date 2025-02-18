package com.avaj;

import com.avaj.trading.broker.Broker;

public class Main {
    public static void main(String[] args) {
        Broker broker = new Broker();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            broker.shutdown();
            System.out.println("Application shutting down...");
        }));

        broker.start();
    }
}