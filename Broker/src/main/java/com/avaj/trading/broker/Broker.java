package com.avaj.trading.broker;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Broker {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5000;
    private static String brokerId;
    private static ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        try (Socket socket = new Socket(ROUTER_HOST, ROUTER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            // Router'dan ID al
            String response = in.readLine();
            if (response.startsWith("ASSIGNED_ID:")) {
                brokerId = response.split(":")[1];
                System.out.println("Broker ID: " + brokerId);
            } else {
                System.out.println("Failed to receive ID from Router.");
                return;
            }

            while (true) {
                System.out.println("Enter order type (BUY/SELL), instrument, quantity, market, price:");
                String orderTypeStr = scanner.next();
                String instrument = scanner.next();
                int quantity = scanner.nextInt();
                String market = scanner.next();
                double price = scanner.nextDouble();

                OrderType orderType;
                try {
                    orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid order type! Use BUY or SELL.");
                    continue;
                }

                FixMessage order = new FixMessage(brokerId, orderType, instrument, quantity, market, price);

                // Executor Service ile işlemi paralel olarak çalıştır
                executor.submit(() -> {
                    try {
                        out.println(order.toFixString());

                        // Market’ten gelen cevabı oku
                        String marketResponse = in.readLine();
                        System.out.println("Market Response: " + marketResponse);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
