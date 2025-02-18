package com.avaj.trading.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketManager {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5000;

    private SocketChannel socketChannel;
    private Selector selector;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private Broker broker;

    public SocketManager(Broker broker) {
        this.broker = broker;
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
                selector.select(); // event yok ise bekler.
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
            System.err.println("Socket event loop error: " + e.getMessage());
            e.printStackTrace();
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
            if (bytesRead > 0) {
                buffer.flip();
                String response = new String(buffer.array(), 0, buffer.limit());
                broker.processServerResponse(response);
            }
        } catch (IOException e) {
            System.err.println("Error reading message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        executor.submit(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(buffer);
                System.out.println("Sent Order: " + message);
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            System.err.println("Error shutting down SocketManager: " + e.getMessage());
        }
    }
}
