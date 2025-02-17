package com.avaj.trading.router;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Router {
    private static final int BROKER_PORT = 5000;
    private static final int MARKET_PORT = 5001;

    private static final Map<Integer, Socket> brokerConnections = new ConcurrentHashMap<>();
    private static final Map<Integer, Socket> marketConnections = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final Random random = new Random();

    public static void main(String[] args) {
        executor.execute(() -> startServer(BROKER_PORT, brokerConnections, "Broker"));
        executor.execute(() -> startServer(MARKET_PORT, marketConnections, "Market"));

        executor.shutdown();
    }

    private static void startServer(int port, Map<Integer, Socket> connections, String type) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(type + " Server running on port: " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                int id = 100000 + random.nextInt(900000);
                connections.put(id, socket);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("ASSIGNED_ID:" + id);

                System.out.println(type + " connected with ID: " + id);
                new Thread(new RouterHandler(socket, connections)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
