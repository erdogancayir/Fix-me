package com.avaj;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class IDatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:fixme.db";

    protected void createTables() {
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_type TEXT,
                instrument TEXT,
                quantity INTEGER,
                market TEXT,
                price REAL,
                checksum TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                status TEXT
            );
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactionsTable);
            System.out.println("Database and tables initialized.");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    public void insertTransaction(String orderType, String instrument, int quantity, String market, double price, String checksum, String status) {
        String sql = "INSERT INTO transactions (order_type, instrument, quantity, market, price, checksum, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderType);
            pstmt.setString(2, instrument);
            pstmt.setInt(3, quantity);
            pstmt.setString(4, market);
            pstmt.setDouble(5, price);
            pstmt.setString(6, checksum);
            pstmt.setString(7, status);
            pstmt.executeUpdate();
            System.out.println("Transaction saved: " + orderType + " " + instrument);
        } catch (SQLException e) {
            System.err.println("Error inserting transaction: " + e.getMessage());
        }
    }

    public List<String> getTransactionsByStatus(String status) {
        List<String> unfinishedTransactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE status = " + status;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                unfinishedTransactions.add(
                        rs.getInt("id") + "|" +
                                rs.getString("order_type") + "|" +
                                rs.getString("instrument") + "|" +
                                rs.getInt("quantity") + "|" +
                                rs.getString("market") + "|" +
                                rs.getDouble("price") + "|" +
                                rs.getString("checksum")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching unfinished transactions: " + e.getMessage());
        }

        return unfinishedTransactions;
    }

    public void updateTransactionStatus(int id, String status) {
        String sql = "UPDATE transactions SET status = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            System.out.println("Transaction ID " + id + " updated to " + status);
        } catch (SQLException e) {
            System.err.println("Error updating transaction: " + e.getMessage());
        }
    }
}