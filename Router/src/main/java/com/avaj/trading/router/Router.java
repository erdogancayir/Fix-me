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
            System.err.println("Error starting router: " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public void shutdown() {
        socketManager.shutdown();
    }
}
