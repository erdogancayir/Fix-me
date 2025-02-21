package com.avaj.trading.broker;

import com.avaj.BrokerDatabaseManager;

import java.util.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrokerSocketManager {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5000;

    private SocketChannel socketChannel;
    private Selector selector;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Broker broker;
    private final BrokerDatabaseManager brokerDatabaseManager;

    public BrokerSocketManager(Broker broker) {
        this.broker = broker;
        this.brokerDatabaseManager = new BrokerDatabaseManager();
    }

    public void startConnection() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(ROUTER_HOST, ROUTER_PORT));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        executor.submit(this::eventLoop);
    }

    private void eventLoop() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        System.err.println("Router connection lost. Exiting Broker...");
                        shutdown();
                        System.exit(1);
                    }

                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        readMessage();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Socket event loop error: " + e.getMessage());
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        if (socketChannel.finishConnect()) {
            key.interestOps(SelectionKey.OP_READ);
            System.out.println("Connected to Router.");

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            socketChannel.read(buffer);
            buffer.flip();
            String response = new String(buffer.array(), 0, buffer.limit());

            if (response.startsWith("ASSIGNED_ID:")) {
                String brokerId = response.split(":")[1].trim();
                broker.setBrokerId(brokerId);
                System.out.println("Broker ID: " + brokerId);
            } else {
                System.out.println("Failed to receive ID from Router.");
            }
        }
    }

    private void readMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            buffer.clear();
            int bytesRead = socketChannel.read(buffer);

            if (bytesRead == -1) {
                System.err.println("Router connection lost. Shutting down Broker...");
                shutdown();
                System.exit(1);
                return;
            }

            if (bytesRead > 0) {
                buffer.flip();
                String response = new String(buffer.array(), 0, buffer.limit());
                broker.processServerResponse(response);
            }
        } catch (IOException e) {
            System.err.println("Error reading message: " + e.getMessage());
        }
    }

    public void sendMessage(FixMessage order) {
        executor.submit(() -> {
            String message = order.toFixString();
            try {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(buffer);
                System.out.println("Sent Order: " + message);

                brokerDatabaseManager.insertTransaction(
                        order.orderType.toString(),
                        order.instrument,
                        order.quantity,
                        order.market,
                        order.price,
                        order.checksum);

            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        });
    }

    private void recoverPendingTransactions() {
//        List<FixMessage> pendingOrders = brokerDatabaseManager.getPendingTransactions();
//        for (FixMessage order : pendingOrders) {
//            System.out.println("Recovering Pending Order: " + order.toFixString());
//            sendMessage(order);
//        }
    }

    public void shutdown() {
        try {
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            System.err.println("Error shutting down SocketManager: " + e.getMessage());
        }

        executor.shutdown();
    }
}
