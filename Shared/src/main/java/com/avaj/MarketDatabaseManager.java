package com.avaj;

import java.util.List;

public class MarketDatabaseManager extends IDatabaseManager
{
    public final String executed = "executed";
    public final String processing = "processing";
    public MarketDatabaseManager()
    {
    }

    public void insertProcessingTransaction(String orderType, String instrument, int quantity, String market, double price, String checksum)
    {
        insertTransaction(orderType, instrument, quantity, market, price, checksum, processing);
    }

    public void updateExecutedTransactionStatus(int id)
    {
        updateTransactionStatus(id, executed);
    }

    public List<String> geProcessingTransactions()
    {
        return getTransactionsByStatus(processing);
    }
}
