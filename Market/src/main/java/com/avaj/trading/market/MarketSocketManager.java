package com.avaj.trading.market;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketSocketManager {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5001;

    private SocketChannel socketChannel;
    private Selector selector;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Market market;
    private volatile boolean running = true;

    public MarketSocketManager(Market market) {
        this.market = market;
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
            while (running) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isReadable()) {
                        readMessage();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error event loop: " + e.getMessage());
        } finally {
            shutdown();
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
                String marketId = response.split(":")[1].trim();
                market.setMarketId(marketId);
                System.out.println("Market ID: " + marketId);
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
                System.err.println("Router disconnected...");
                shutdown();
                System.exit(1);
                return;
            }

            if (bytesRead > 0) {
                buffer.flip();
                String message = new String(buffer.array(), 0, buffer.limit());

                if (message.startsWith("ASSIGNED_ID:") && market.getMarketId() == null) {
                    String marketId = message.split(":")[1].trim();
                    market.setMarketId(marketId);
                    System.out.println("Market ID set: " + marketId);
                    return;
                }

                System.out.println("Received Order: " + message);
                executor.submit(() -> market.processOrder(message));
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        executor.submit(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(buffer);
                System.out.println("Response Sent: " + message);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        System.out.println("Shutting down MarketSocketManager...");
        running = false;
        try {
            if (selector != null) {
                selector.close();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }
}
