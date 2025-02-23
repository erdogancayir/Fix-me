package com.avaj.trading.broker;

import java.util.Scanner;

public class Broker {
    private String brokerId;
    private final BrokerSocketManager socketManager;

    public Broker() {
        socketManager = new BrokerSocketManager(this);
    }

    public void start() {
        try {
            socketManager.startConnection();

            while (brokerId == null) {
                Thread.sleep(100);
            }

            listenForUserInput();
        } catch (Exception e) {
            System.err.println("Error starting broker: " + e.getMessage());
        }
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public void processServerResponse(String response) {
        System.out.println("Market Response: " + response);
    }

    private void listenForUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter order type (BUY/SELL), instrument, quantity, market, price:");
            String orderTypeStr = scanner.next();
            String instrument = scanner.next();
            int quantity = scanner.nextInt();
            String market = scanner.next();
            double price = scanner.nextDouble();

            OrderType orderType = parseOrderType(orderTypeStr);
            if (orderType == null) {
                System.out.println("Invalid order type! Use BUY or SELL.");
                continue;
            }

            FixMessage order = new FixMessage(brokerId, orderType, instrument, quantity, market, price);
            socketManager.sendMessage(order.toFixString());
            socketManager.insertTransaction(order.toFixString());
        }
    }

    private OrderType parseOrderType(String orderTypeStr) {
        try {
            return OrderType.valueOf(orderTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void shutdown() {
        socketManager.shutdown();
    }
}
