package com.avaj.trading.MessageHandler;

import com.avaj.trading.router.RouterSocketManager;

public class BrokerMessageHandler implements IMessageHandler {
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
            System.err.println("Geçersiz mesaj formatı: " + message);
            return;
        }

        try {
            int senderId = Integer.parseInt(parts[0]);
            if (router.isBroker(senderId)) {
                int marketId = router.getMarketForBroker(senderId);
                if (marketId == -1) {
                    System.err.println("No available market for order: " + message);
                    return;
                }

                router.sendMessage(marketId, message);
            } else if (next != null) {
                next.handle(message, router);
            }
        } catch (NumberFormatException e) {
            System.err.println("Geçersiz Broker ID formatı: " + message);
        }
    }
}
