package com.avaj.trading.database;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:trading.db";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "broker_id TEXT," +
                    "order_type TEXT," +
                    "instrument TEXT," +
                    "quantity INTEGER," +
                    "market TEXT," +
                    "price REAL," +
                    "status TEXT," +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveTransaction(String brokerId, String orderType, String instrument, int quantity, String market, double price, String status) {
        String sql = "INSERT INTO transactions (broker_id, order_type, instrument, quantity, market, price, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, brokerId);
            pstmt.setString(2, orderType);
            pstmt.setString(3, instrument);
            pstmt.setInt(4, quantity);
            pstmt.setString(5, market);
            pstmt.setDouble(6, price);
            pstmt.setString(7, status);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

