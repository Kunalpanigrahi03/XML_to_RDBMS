package org.example;

import java.sql.*;

public class DBConnection {
    public static Connection conn = null;

    public static void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/mysql?useSSL=false";
            String user = "root";
            String password = "2003kunal";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection established: " + conn);
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("SQL error occurred.");
            e.printStackTrace();
        }
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
        return conn;
    }
}