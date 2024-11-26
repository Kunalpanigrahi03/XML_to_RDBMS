package org.example;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class ExistingSchemaHandler {
    public static void loadIntoExistingSchema(String xmlFilePath, String tableName)
            throws ParserConfigurationException, SAXException, IOException, SQLException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFilePath);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        NodeList records = root.getChildNodes();

        Map<String, String> existingColumns = getExistingTableStructure(tableName);
        insertValuesIntoExistingTable(tableName, records, existingColumns);
    }

    public static Map<String, String> getExistingTableStructure(String tableName) throws SQLException {
        Map<String, String> columns = new HashMap<>();
        Connection conn = DBConnection.getConnection();
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet result = metaData.getColumns(null, null, tableName, null);

        while (result.next()) {
            String columnName = result.getString("COLUMN_NAME");
            String columnType = result.getString("TYPE_NAME");
            columns.put(columnName, columnType);
        }
        return columns;
    }

    private static void insertValuesIntoExistingTable(String tableName, NodeList records, Map<String, String> existingColumns)
            throws SQLException {
        Connection conn = DBConnection.getConnection();
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ");
        insertSQL.append(tableName).append(" (");

        for (String columnName : existingColumns.keySet()) {
            insertSQL.append("`").append(columnName).append("`, ");
        }

        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(") VALUES (");

        insertSQL.append("?, ".repeat(existingColumns.size()));

        insertSQL.setLength(insertSQL.length() - 2);
        insertSQL.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(insertSQL.toString())) {
            for (int i = 0; i < records.getLength(); i++) {
                Node record = records.item(i);
                if (record.getNodeType() == Node.ELEMENT_NODE) {
                    Element recordElement = (Element) record;
                    int parameterIndex = 1;

                    for (Map.Entry<String, String> column : existingColumns.entrySet()) {
                        String columnName = column.getKey();
                        String value = null;

                        if (columnName.startsWith("attr_")) {
                            value = recordElement.getAttribute(columnName.substring(5));
                        }

                        if ((value == null || value.isEmpty()) && !columnName.startsWith("attr_")) {
                            NodeList elements = recordElement.getElementsByTagName(columnName);
                            if (elements.getLength() > 0) {
                                value = elements.item(0).getTextContent();
                            }
                        }

                        if (value != null && !value.isEmpty()) {
                            setAppropriateValue(stmt, parameterIndex, value, column.getValue());
                        } else {
                            stmt.setNull(parameterIndex, Types.VARCHAR);
                        }
                        parameterIndex++;
                    }
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            System.out.println("Inserted records into " + tableName);
        }
    }

    public static void setAppropriateValue(PreparedStatement stmt, int parameterIndex, String value, String columnType) throws SQLException {
        if (value == null || value.isEmpty()) {
            stmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            switch (columnType) {
                case "INT" -> stmt.setInt(parameterIndex, Integer.parseInt(value));
                case "BIGINT" -> stmt.setLong(parameterIndex, Long.parseLong(value));
                case "DECIMAL(10,2)" -> stmt.setBigDecimal(parameterIndex, new BigDecimal(value));
                case "DATE" -> {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        java.util.Date parsedDate = dateFormat.parse(value.trim());
                        Date sqlDate = new Date(parsedDate.getTime());
                        stmt.setDate(parameterIndex, sqlDate);
                    } catch (ParseException e) {
                        System.out.println("Error parsing date value: '" + value + "'. Setting as NULL");
                        stmt.setNull(parameterIndex, Types.DATE);
                    }
                }
                default -> stmt.setString(parameterIndex, value);
            }
        }
    }
}