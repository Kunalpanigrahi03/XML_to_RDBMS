package org.example;

import java.sql.*;
import java.util.Map;

public class createTable {
    public static void tableCreation(String tableName, Map<String, String> columns) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + tableName);
        }

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE ");
        createTableSQL.append(tableName).append(" (");

        String primaryKey = findPrimaryKey(columns, tableName);

        for (Map.Entry<String, String> column : columns.entrySet()) {
            String columnName = column.getKey();
            String columnType = column.getValue();

            if (columnType.startsWith("VARCHAR") && columnType.length() > 12) {
                columnType = "VARCHAR(255)";
            }

            if (columnType.equals("INT") || columnType.equals("DECIMAL(10,2)")) {
                columnType = "VARCHAR(255)";
            }

            createTableSQL.append("`")
                    .append(columnName)
                    .append("` ")
                    .append(columnType);

            if (columnName.equals(primaryKey)) {
                createTableSQL.append(" NOT NULL PRIMARY KEY");
            }

            createTableSQL.append(", ");
        }

        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL.toString());
            System.out.println("Table " + tableName + " created successfully");
        }
    }

    static String findPrimaryKey(Map<String, String> columns, String tableName) {
        for (String columnName : columns.keySet()) {
            if (columnName.equalsIgnoreCase("id")) {
                return "id";
            }
        }

        for (String columnName : columns.keySet()) {
            if (columnName.equalsIgnoreCase("attr_id")) {
                return "attr_id";
            }
        }

        String tableIdColumn = tableName.toLowerCase() + "_id";
        for (String columnName : columns.keySet()) {
            if (columnName.toLowerCase().equals(tableIdColumn)) {
                return columnName;
            }
        }

        return columns.keySet().iterator().next();
    }
}