package com.avaj.trading.broker;

import java.util.InputMismatchException;
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
            try {
                System.out.println("\n📌 Enter order type (BUY/SELL), instrument, quantity, market, price:");

                String orderTypeStr = getValidString(scanner, "Order Type (BUY/SELL)");
                OrderType orderType = parseOrderType(orderTypeStr);
                if (orderType == null) {
                    System.err.println("❌ Invalid order type! Use BUY or SELL.");
                    continue;
                }

                String instrument = getValidString(scanner, "Instrument");
                int quantity = getValidInt(scanner, "Quantity");
                String market = getValidString(scanner, "Market");
                double price = getValidDouble(scanner, "Price");

                FixMessage order = new FixMessage(brokerId, orderType, instrument, quantity, market, price);
                socketManager.sendMessage(order.toFixString());
                socketManager.insertTransaction(order.toFixString());

                System.out.println("✅ Order Sent: " + order.toFixString());

            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
                scanner.nextLine();
            }
        }
    }

    private String getValidString(Scanner scanner, String fieldName) {
        while (true) {
            System.out.print("🔹 " + fieldName + ": ");
            String input = scanner.next().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.err.println("❌ " + fieldName + " cannot be empty. Try again.");
        }
    }

    private int getValidInt(Scanner scanner, String fieldName) {
        while (true) {
            System.out.print("🔹 " + fieldName + ": ");
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                System.err.println("❌ Invalid number! Please enter a valid integer.");
                scanner.nextLine();
            }
        }
    }

    private double getValidDouble(Scanner scanner, String fieldName) {
        while (true) {
            System.out.print("🔹 " + fieldName + ": ");
            try {
                return scanner.nextDouble();
            } catch (InputMismatchException e) {
                System.err.println("❌ Invalid number! Please enter a valid decimal value.");
                scanner.nextLine();
            }
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
