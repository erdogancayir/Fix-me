package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class IDatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:../Shared/fixme.db";

    public IDatabaseManager() {
        createTables();
    }

    private void createTables() {
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message TEXT NOT NULL,   
                status TEXT NOT NULL,    
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactionsTable);
            System.out.println("✅ Database initialized.");
        } catch (SQLException e) {
            System.err.println("❌ Error creating tables: " + e.getMessage());
        }
    }

    /**
     * Adds a new transaction.
     * @param message - Order in the FixMessage format
     * @param status - pending, processing, executed
     */
    public void insertTransaction(String message, String status) {
        String sql = "INSERT INTO transactions (message, status) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.setString(2, status);
            pstmt.executeUpdate();
            System.out.println("✅ Transaction inserted: " + message);
        } catch (SQLException e) {
            System.err.println("❌ Error inserting transaction: " + e.getMessage());
        }
    }

    /**
     * Pulls transactions with certain status.
     * @param status - "pending", "processing", "executed"
     * @return List of process messages
     */
    public List<String> getTransactionsByStatus(String status) {
        List<String> transactions = new ArrayList<>();
        String sql = "SELECT message FROM transactions WHERE status = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(rs.getString("message"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching transactions: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Updates the status of the specified message.
     * @param message - Order in the FixMessage format
     * @param newStatus - New status (processing, executed)
     */
    public void updateTransactionStatus(String message, String newStatus) {
        String sql = "UPDATE transactions SET status = ? WHERE message = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            System.out.println("✅ Transaction updated to " + newStatus + " for: " + message);
        } catch (SQLException e) {
            System.err.println("❌ Error updating transaction status: " + e.getMessage());
        }
    }

    public boolean transactionExists(String message) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE message = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking transaction existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all transactions from the database.
     * Call this in tests to clean up after execution.
     */
    public void clearDatabase() {
        String sql = "DELETE FROM transactions";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("🗑 Database cleared after test.");
        } catch (SQLException e) {
            System.err.println("❌ Error clearing database: " + e.getMessage());
        }
    }
    }