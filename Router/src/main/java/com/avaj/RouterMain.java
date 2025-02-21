package com.avaj;

import com.avaj.trading.router.Router;

public class RouterMain {
    public static void main(String[] args) {
        Router router = new Router();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            router.shutdown();
            System.out.println("Application shutting down...");
        }));

        router.start();
    }
}
