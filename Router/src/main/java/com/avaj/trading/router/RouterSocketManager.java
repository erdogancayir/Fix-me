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
            clientChannel.register(selector, SelectionKey.OP_READ);

            String type = (String) key.attachment();
            int id = 100000 + random.nextInt(900000);
            if (id % 2 != 0 && brokerConnections.containsKey(id) && marketConnections.containsKey(id))
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
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                cleanupConnection(clientChannel, key);
                return;
            }

            buffer.flip();
            String message = new String(buffer.array(), 0, buffer.limit());
            System.out.println("Received: " + message);

            if (!ChecksumValidator.isValid(message)) {
                System.err.println("Invalid checksum! Message rejected.");
                return;
            }

            forwardMessage(message);
        } catch (IOException e) {
            System.err.println("Error reading message: " + e.getMessage());
        }
    }

    public void forwardMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 2) {
            System.err.println("Geçersiz mesaj formatı: " + message);
            return;
        }

        try {
            int senderId = Integer.parseInt(parts[0]); // Mesaj gönderen ID

            if (brokerConnections.containsKey(senderId)) {
                // Broker’dan gelen mesaj
                int marketId = brokerMarketMap.getOrDefault(senderId, findAvailableMarket());
                if (marketId == -1) {
                    System.err.println("No available market for order: " + message);
                    return;
                }

                // Broker ID ile Market’i eşle
                brokerMarketMap.put(senderId, marketId);

                // Broker mesajını Market’e yönlendir
                String updatedMessage = message.substring(parts[0].length() + 1); // Broker ID kaldırıldı
                sendMessage(marketId, updatedMessage);
            }
            else if (marketConnections.containsKey(senderId)) {
                // Market’ten gelen mesaj
                int brokerId = getBrokerByMarket(senderId);
                if (brokerId == -1) {
                    System.err.println("No broker found for market response: " + message);
                    return;
                }

                // Market mesajını Broker’a yönlendir
                sendMessage(brokerId, message);
            }
            else {
                System.err.println("Unknown sender ID: " + senderId);
            }
        } catch (NumberFormatException e) {
            System.err.println("Geçersiz ID formatı: " + parts[0]);
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
                .findAny() // rastgele bir Market seçelim
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

    private void cleanupConnection(SocketChannel clientChannel, SelectionKey key) {
        key.cancel();
        try {
            clientChannel.close();
        } catch (IOException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }

        Integer disconnectedId = null;
        for (var entry : brokerConnections.entrySet()) {
            if (entry.getValue().equals(clientChannel)) {
                disconnectedId = entry.getKey();
                brokerConnections.remove(entry.getKey());
                break;
            }
        }

        for (var entry : marketConnections.entrySet()) {
            if (entry.getValue().equals(clientChannel)) {
                disconnectedId = entry.getKey();
                marketConnections.remove(entry.getKey());
                break;
            }
        }

        System.out.println("Bağlantı kapandı: " + disconnectedId);

        // Tüm bağlantılara bağlantının kesildiğini bildiren mesaj gönder
        if (disconnectedId != null) {
            String disconnectMessage = "DISCONNECT|" + disconnectedId;
            brokerConnections.values().forEach(socket -> sendMessageToSocket(socket, disconnectMessage));
            marketConnections.values().forEach(socket -> sendMessageToSocket(socket, disconnectMessage));
        }
    }

    // Yardımcı metod: Socket'e mesaj gönderme
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
        selector.wakeup(); // runEventLoop() içindeki select()'i kırmak için

        cleanup();
    }

    private void cleanup() {
        try {
            selector.close();
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
}
