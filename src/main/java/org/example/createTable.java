package org.example;

import java.sql.*;
import java.util.Map;

public class createTable {
    public static void createTable(String tableName, String recordTag, Map<String, String> columns) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + tableName);
        }

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
        createTableSQL.append(tableName).append(" (");

        createTableSQL.append(recordTag).append(" VARCHAR(255) PRIMARY KEY, ");

        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (!column.getKey().equals(recordTag)) {
                createTableSQL.append("`")
                        .append(column.getKey())
                        .append("` ")
                        .append(column.getValue())
                        .append(", ");
            }
        }

        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL.toString());
            System.out.println("Table " + tableName + " created successfully");
        }
    }
}