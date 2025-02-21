package com.avaj;

import java.util.List;

public class BrokerDatabaseManager extends IDatabaseManager
{
    public final String pending = "pending";
    public BrokerDatabaseManager()
    {
    }

    public void insertTransaction(String orderType, String instrument, int quantity, String market, double price, String checksum)
    {
        insertTransaction(orderType, instrument, quantity, market, price, checksum, pending);
    }

    public List<String> gePendingTransactions()
    {
        return getTransactionsByStatus(pending);
    }
}
