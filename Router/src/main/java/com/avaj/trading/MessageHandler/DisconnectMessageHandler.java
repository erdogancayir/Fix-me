package com.avaj.trading.MessageHandler;

import com.avaj.trading.router.RouterSocketManager;

public class DisconnectMessageHandler implements IMessageHandler {
    private IMessageHandler next;

    @Override
    public IMessageHandler setNext(IMessageHandler next) {
        this.next = next;
        return next;
    }

    @Override
    public void handle(String message, RouterSocketManager router) {
        if (message.startsWith("DISCONNECT|")) {
            try {
                int disconnectedId = Integer.parseInt(message.split("\\|")[1]);
                router.cleanupConnection(disconnectedId);
                System.out.println("Processed disconnection for: " + disconnectedId);
            } catch (NumberFormatException e) {
                System.err.println("Invalid disconnect message format: " + message);
            }
        } else if (next != null) {
            next.handle(message, router);
        }
    }
}