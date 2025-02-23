package database;

import java.util.List;

public class MarketDatabaseManager extends IDatabaseManager
{
    private static final String PROCESSING_STATUS = "processing";
    private static final String COMPLETED_STATUS = "completed";
    private static final String NotFixFORMAT_STATUS = "notFixFormat";

    public MarketDatabaseManager() {
        super();
    }

    public List<String> getProcessingTransactions() {
        return getTransactionsByStatus(PROCESSING_STATUS);
    }

    public void markAsCompleted(String message) {
        if (transactionExists(message)) {
            updateTransactionStatus(message, COMPLETED_STATUS);
        }
        else {
            System.err.println("Transaction not found for executed: " + message);
        }
    }

    public void markAsNotFixFormat(String message) {
        if (transactionExists(message)) {
            updateTransactionStatus(message, NotFixFORMAT_STATUS);
        }
        else {
            System.err.println("Transaction not found for NotFixFormat: " + message);
        }
    }

    public void markAsProcessing(String message) {
        if (transactionExists(message)) {
            updateTransactionStatus(message, PROCESSING_STATUS);
        }
        else {
            System.err.println("Transaction not found for processing: " + message);
        }
    }
}