package com.avaj.trading.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class RouterSocketManager {
    private static final int BROKER_PORT = 5000;
    private static final int MARKET_PORT = 5001;

    private final Map<Integer, SocketChannel> brokerConnections = new ConcurrentHashMap<>();
    private final Map<Integer, SocketChannel> marketConnections = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> brokerMarketMap = new ConcurrentHashMap<>();

    private Selector selector;
    private final Router router;
    private volatile boolean running = true;
    private static final Random random = new Random();

    public RouterSocketManager(Router router){
        this.router = router;
    }

    public void startServers() throws IOException {
        this.selector = Selector.open();

        startServer(BROKER_PORT, brokerConnections, "Broker");
        startServer(MARKET_PORT, marketConnections, "Market");

        runEventLoop();
    }

    private void startServer(int port, Map<Integer, SocketChannel> connections, String type) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, type);
        System.out.println(type + " Server running on port: " + port);
    }

    private void runEventLoop() {
        while (running) {
            try {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        acceptConnection((ServerSocketChannel) key.channel(), key);
                    } else if (key.isReadable()) {
                        readMessage(key);
                    }
                }
            } catch (IOException e) {
                if (!running) {
                    System.out.println("Router shutting down...");
                } else {
                    System.err.println("Selector error: " + e.getMessage());
                }
            }
        }
        cleanup();
    }

    private void acceptConnection(ServerSocketChannel serverChannel, SelectionKey key) {
        try {
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            String type = (String) key.attachment();
            int id = 100000 + random.nextInt(900000);
            if (id % 2 != 0 || brokerConnections.containsKey(id) || marketConnections.containsKey(id))
                id++;

            if (serverChannel.socket().getLocalPort() == BROKER_PORT) {
                brokerConnections.put(id, clientChannel);
            } else {
                marketConnections.put(id, clientChannel);
            }

            ByteBuffer buffer = ByteBuffer.wrap(("ASSIGNED_ID:" + id).getBytes());
            clientChannel.write(buffer);
            System.out.println("New " + type + " connection assigned ID: " + id);
        } catch (IOException e) {
            System.err.println("Failed to accept connection: " + e.getMessage());
        }
    }

    private void readMessage(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try {
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                int disconnectedId = findClientId(clientChannel);
                if (disconnectedId != -1) {
                    cleanupConnection(disconnectedId);
                }
                return;
            }

            if (bytesRead == 0) {
                return;
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data).trim();

            System.out.println("Received: " + message);

            if (!ChecksumValidator.isValid(message)) {
                System.err.println("Invalid checksum! Message rejected.");
                return;
            }

            forwardMessage(message);
        } catch (IOException e) {
            System.err.println("Error reading message: " + e.getMessage());
            int disconnectedId = findClientId(clientChannel);
            if (disconnectedId != -1) {
                forwardMessage("DISCONNECT|" + disconnectedId);
            }
        }
    }

    public void forwardMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 2) {
            System.err.println("Invalid message format: " + message);
            return;
        }

        try {
            int senderId = Integer.parseInt(parts[0]); // sent id

            if (brokerConnections.containsKey(senderId)) {
                int marketId = brokerMarketMap.getOrDefault(senderId, findAvailableMarket());
                if (marketId == -1) {
                    System.err.println("No available market for order: " + message);
                    return;
                }

                brokerMarketMap.put(senderId, marketId);

                //String updatedMessage = message.substring(parts[0].length() + 1); // Broker ID kaldırıldı
                sendMessage(marketId, message);
            }
            else if (marketConnections.containsKey(senderId)) {
                int brokerId = getBrokerByMarket(senderId);
                if (brokerId == -1) {
                    System.err.println("No broker found for market response: " + message);
                    return;
                }

                sendMessage(brokerId, message);
            }
            else {
                System.err.println("Unknown sender ID: " + senderId);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid ID format: " + parts[0]);
        }
    }

    public void sendMessage(int targetId, String message) {
        SocketChannel targetSocket = brokerConnections.getOrDefault(targetId, marketConnections.get(targetId));

        if (targetSocket != null) {
            try {
                targetSocket.write(ByteBuffer.wrap(message.getBytes()));
                System.out.println("Message forwarded to ID: " + targetId);
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        } else {
            System.err.println("Unknown target ID: " + targetId);
        }
    }

    private int findAvailableMarket() {
        return marketConnections.keySet()
                .stream()
                .findAny() // Let's choose a random market
                .orElse(-1);
    }

    private int getBrokerByMarket(int marketId) {
        return brokerMarketMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(marketId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    public void cleanupConnection(int disconnectedId) {
        SocketChannel clientChannel = brokerConnections.remove(disconnectedId);

        if (clientChannel == null) {
            clientChannel = marketConnections.remove(disconnectedId);
        }

        if (clientChannel != null) {
            try {
                clientChannel.close();
            } catch (IOException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }

            // 📌 Eğer bağlantı kopan broker ise, routing table'dan temizleyelim
            if (brokerMarketMap.containsKey(disconnectedId)) {
                int marketId = brokerMarketMap.remove(disconnectedId);
                System.out.println("📌 Broker " + disconnectedId + " disconnected. Unmapped from Market " + marketId);
            }

            // 📌 Eğer bağlantı kopan market ise, bağlı brokerları kontrol edelim
            brokerMarketMap.entrySet().removeIf(entry -> {
                if (entry.getValue().equals(disconnectedId)) {
                    System.out.println("📌 Market " + disconnectedId + " disconnected. Unmapping Broker " + entry.getKey());
                    return true;
                }
                return false;
            });

            System.out.println("✅ Connection closed: " + disconnectedId);

            // 📌 Diğer istemcilere bağlantının kesildiğini bildir
            String disconnectMessage = "DISCONNECT|" + disconnectedId;
            brokerConnections.values().forEach(socket -> sendMessageToSocket(socket, disconnectMessage));
            marketConnections.values().forEach(socket -> sendMessageToSocket(socket, disconnectMessage));
        } else {
            System.err.println("Unknown disconnect request for ID: " + disconnectedId);
        }
    }

    private void sendMessageToSocket(SocketChannel socket, String message) {
        try {
            socket.write(ByteBuffer.wrap(message.getBytes()));
        } catch (IOException e) {
            System.err.println("Bağlantı kesildi mesajı gönderilemedi: " + e.getMessage());
        }
    }

    public void shutdown() {
        System.out.println("Shutting down RouterSocketManager...");

        running = false;

        if (selector != null && selector.isOpen()) {
            selector.wakeup();
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println("Error closing selector: " + e.getMessage());
            }
        }

        cleanup();
    }


    private void cleanup() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        }

        brokerConnections.values().forEach(socket -> {
            try {
                socket.close();
            } catch (IOException ignored) {}
        });

        marketConnections.values().forEach(socket -> {
            try {
                socket.close();
            } catch (IOException ignored) {}
        });

        brokerConnections.clear();
        marketConnections.clear();
        System.out.println("Router fully shut down.");
    }

    private int findClientId(SocketChannel clientChannel) {
        for (var entry : brokerConnections.entrySet()) {
            if (entry.getValue().equals(clientChannel)) {
                return entry.getKey();
            }
        }
        for (var entry : marketConnections.entrySet()) {
            if (entry.getValue().equals(clientChannel)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public boolean isBroker(int id) {
        return brokerConnections.containsKey(id);
    }

    public boolean isMarket(int id) {
        return marketConnections.containsKey(id);
    }

    public int getMarketForBroker(int brokerId) {
        return brokerMarketMap.getOrDefault(brokerId, findAvailableMarket());
    }

    public int getBrokerForMarket(int marketId) {
        return brokerMarketMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(marketId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }
}
