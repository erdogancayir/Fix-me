package database;

import java.util.List;

public class BrokerDatabaseManager extends IDatabaseManager {
    private static final String PENDING_STATUS = "pending";

    public BrokerDatabaseManager() {
        super();
    }

    public void brokerInsertTransaction(String message) {
        insertTransaction(message, PENDING_STATUS);
    }

    public List<String> getPendingTransactions() {
        return getTransactionsByStatus(PENDING_STATUS);
    }
}