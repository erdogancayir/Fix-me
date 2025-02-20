package com.avaj.trading.MessageHandler;

import com.avaj.trading.router.RouterSocketManager;

public interface IMessageHandler {
    IMessageHandler setNext(IMessageHandler next);
    void handle(String message, RouterSocketManager router);
}
