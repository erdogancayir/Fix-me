package com.avaj.trading.MessageHandler;

import com.avaj.trading.router.RouterSocketManager;

public class MarketMessageHandler implements IMessageHandler {
    private IMessageHandler next;

    @Override
    public IMessageHandler setNext(IMessageHandler next) {
        this.next = next;
        return next;
    }

    @Override
    public void handle(String message, RouterSocketManager router) {
        String[] parts = message.split("\\|");

        if (parts.length < 2) {
            System.err.println("Invalid message format: " + message);
            return;
        }

        try {
            int senderId = Integer.parseInt(parts[0]);
            if (router.isMarket(senderId)) {
                int brokerId = router.getBrokerForMarket(senderId);
                if (brokerId == -1) {
                    System.err.println("No broker found for market response: " + message);
                    return;
                }

                router.sendMessage(brokerId, message);
            } else if (next != null) {
                next.handle(message, router);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid Market ID format: " + message);
        }
    }
}
