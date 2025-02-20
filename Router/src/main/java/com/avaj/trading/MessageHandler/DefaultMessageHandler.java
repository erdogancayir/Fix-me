package com.avaj.trading.MessageHandler;

import com.avaj.trading.router.RouterSocketManager;

public class DefaultMessageHandler implements IMessageHandler {
    @Override
    public IMessageHandler setNext(IMessageHandler next) {
        return next;
    }

    @Override
    public void handle(String message, RouterSocketManager router) {
        System.err.println("Unhandled message type: " + message);
    }
}
