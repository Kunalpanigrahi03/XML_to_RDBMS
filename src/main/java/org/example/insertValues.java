package org.example;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class insertValues {
    public static void insertAllRecords(String tableName, String recordTag, NodeList records, Map<String, String> columns) throws SQLException {
        Connection conn = DBConnection.getConnection();
        List<String> columnNames = new ArrayList<>(columns.keySet());
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ");
        insertSQL.append(tableName).append(" (");

        List<String> quotedColumns = new ArrayList<>();
        for (String column : columnNames) {
            quotedColumns.add("`" + column + "`");
        }
        insertSQL.append(String.join(", ", quotedColumns));

        insertSQL.append(") VALUES (");
        String[] placeholders = new String[columnNames.size()];
        Arrays.fill(placeholders, "?");
        insertSQL.append(String.join(", ", placeholders));
        insertSQL.append(")");

        System.out.println("SQL Statement: " + insertSQL);

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL.toString())) {
            for (int i = 0; i < records.getLength(); i++) {
                Element record = (Element) records.item(i);
                pstmt.clearParameters();

                for (int j = 0; j < columnNames.size(); j++) {
                    String columnName = columnNames.get(j);
                    String value = null;

                    if (columnName.equals(recordTag)) {
                        value = record.getAttribute("id");
                        if (value.isEmpty()) {
                            value = recordTag + "_" + i;
                        }
                    } else if (columnName.startsWith("attr_")) {
                        value = record.getAttribute(columnName.substring(5));
                    } else {
                        NodeList nodes = record.getElementsByTagName(columnName);
                        if (nodes.getLength() > 0) {
                            StringBuilder multiValue = new StringBuilder();
                            for (int k = 0; k < nodes.getLength(); k++) {
                                if (k > 0) {
                                    multiValue.append(" | ");
                                }
                                Node node = nodes.item(k);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    Element elem = (Element) node;
                                    if (elem.getChildNodes().getLength() > 1) {
                                        multiValue.append(getStructuredContent(elem));
                                    } else {
                                        multiValue.append(elem.getTextContent().trim());
                                    }
                                }
                            }
                            value = multiValue.toString();
                        }
                    }

                    if (value != null) {
                        value = value.trim().replaceAll("\\s+", " ");
                    }

                    if (value == null || value.isEmpty()) {
                        pstmt.setNull(j + 1, Types.VARCHAR);
                    } else {
                        setAppropriateValue(pstmt, j + 1, value, columns.get(columnName));
                    }
                }

                pstmt.executeUpdate();
                System.out.println("Inserted record " + (i + 1) + " of " + records.getLength());
            }
        }
    }

    private static String getStructuredContent(Element element) {
        StringBuilder content = new StringBuilder("{");
        NodeList children = element.getChildNodes();
        boolean first = true;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (!first) {
                    content.append(", ");
                }
                content.append(child.getNodeName())
                        .append(": ")
                        .append(child.getTextContent().trim());
                first = false;
            }
        }
        content.append("}");
        return content.toString();
    }

    private static void setAppropriateValue(PreparedStatement pstmt, int index, String value, String type) throws SQLException {
        try {
            switch (type) {
                case "INT" -> pstmt.setInt(index, Integer.parseInt(value.trim()));
                case "DECIMAL(10,2)" -> pstmt.setDouble(index, Double.parseDouble(value.trim()));
                default -> pstmt.setString(index, value.trim());
            }
        } catch (NumberFormatException e) {
            System.out.println("Error converting value: '" + value + "' to type: " + type);
            pstmt.setString(index, value.trim());
        }
    }
}