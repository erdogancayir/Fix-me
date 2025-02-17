package com.avaj.trading.market;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Market {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5001;
    private static String marketId;
    private static ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        try (Socket socket = new Socket(ROUTER_HOST, ROUTER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Router'dan ID al
            String response = in.readLine();
            if (response.startsWith("ASSIGNED_ID:")) {
                marketId = response.split(":")[1];
                System.out.println("Market ID: " + marketId);
            } else {
                System.out.println("Failed to receive ID from Router.");
                return;
            }

            while (true) {
                String message = in.readLine();
                if (message == null) continue;

                executor.submit(() -> processOrder(message, out));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processOrder(String message, PrintWriter out) {
        System.out.println("Received: " + message);

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
        out.println(responseMessage);
        System.out.println("Response sent: " + responseMessage);
    }

    private static String generateChecksum() {
        return String.valueOf((int) (Math.random() * 10000));
    }
}
