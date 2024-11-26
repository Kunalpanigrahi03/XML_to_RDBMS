package org.example;

import java.sql.*;

public class DBConnection {
    public static Connection conn = null;

    public static Connection connectDB() {
        try {
            String url = "jdbc:mysql://localhost:3306/mysql?useSSL=false";
            String user = "root";
            String password = "2003kunal";
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection established: " + conn);
            return conn;
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("SQL error occurred.");
            e.printStackTrace();
        }
        return null;
    }

    public static void closeDB() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                return connectDB();
            }
            return conn;
        } catch (SQLException e) {
            System.out.println("Error checking connection status.");
            e.printStackTrace();
            return connectDB();
        }
    }
}