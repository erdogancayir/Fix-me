package com.avaj.trading.router;

import java.io.IOException;

public class Router {
    private RouterSocketManager socketManager;

    public Router() {
        this.socketManager = new RouterSocketManager(this);
    }

    public void start() {
        try {
            socketManager.startServers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forwardMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 2) {
            System.err.println("Geçersiz mesaj formatı: " + message);
            return;
        }

        try {
            int targetId = Integer.parseInt(parts[0]);
            socketManager.sendMessage(targetId, message);
        } catch (NumberFormatException e) {
            System.err.println("Geçersiz hedef ID: " + parts[0]);
        }
    }

    public void shutdown() {
        socketManager.shutdown();
    }
}
