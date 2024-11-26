package org.example;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class readXMLfile {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, SQLException {
        if (args.length < 1) {
            System.out.println("Usage: java ReadXMLFile <filename>");
            return;
        }

        String xmlFilePath = args[0];
        xmlValidator.main(new String[]{xmlFilePath});
        DBConnection.connectDB();

        if (args.length > 1 && args[1].equalsIgnoreCase("existing")) {
            if (args.length < 3) {
                System.out.println("Please provide the table name.");
                return;
            }
            String tableName = args[2];
            ExistingSchemaHandler.loadIntoExistingSchema(xmlFilePath, tableName);
            displayTableData(tableName);
        } else {
            processNewXML(xmlFilePath);
        }

        DBConnection.closeDB();
    }

    private static void processNewXML(String xmlFilePath) throws ParserConfigurationException, SAXException, IOException, SQLException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFilePath);
        document.getDocumentElement().normalize();

        Element rootElement = document.getDocumentElement();
        Map<String, TableSchema> tableSchemas = analyzeXMLStructure(rootElement);

        createTables(tableSchemas);
        insertValues.insertRecords(rootElement, tableSchemas);
    }

    private static Map<String, TableSchema> analyzeXMLStructure(Element rootElement) {
        Map<String, TableSchema> tableSchemas = new HashMap<>();
        String rootTableName = rootElement.getNodeName();
        TableSchema rootSchema = new TableSchema(rootTableName, null);

        rootSchema.addColumn("id", "VARCHAR(255) PRIMARY KEY");
        tableSchemas.put(rootTableName, rootSchema);

        analyzeRecursively(rootElement, tableSchemas, null);
        return tableSchemas;
    }

    private static void analyzeRecursively(Element element, Map<String, TableSchema> tableSchemas, String parentTableName) {
        String tableName = element.getNodeName();
        TableSchema schema = tableSchemas.computeIfAbsent(tableName, k -> new TableSchema(k, parentTableName));

        if (!schema.getColumns().containsKey("id")) {
            schema.addColumn("id", "VARCHAR(255) PRIMARY KEY");
        }

        if (parentTableName != null && !schema.getColumns().containsKey("parent_id")) {
            schema.addColumn("parent_id", "VARCHAR(255)");
        }

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String columnName = attr.getNodeName();
            if (!schema.getColumns().containsKey(columnName)) {
                schema.addColumn(columnName, inferDataType(attr.getNodeValue()));
            }
        }

        Map<String, Boolean> complexElementGroups = new HashMap<>();

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                if (isSimpleElement(childElement)) {
                    String columnName = childElement.getNodeName();
                    if (!schema.getColumns().containsKey(columnName)) {
                        schema.addColumn(columnName, inferDataType(childElement.getTextContent()));
                    }
                } else {
                    String complexElementName = childElement.getNodeName();

                    if (!complexElementName.equals(tableName)) {
                        complexElementGroups.put(complexElementName, true);
                    }

                    analyzeRecursively(childElement, tableSchemas, tableName);
                }
            }
        }

        // Dynamically add columns for complex elements
        for (String complexElementName : complexElementGroups.keySet()) {
            if (!schema.getColumns().containsKey(complexElementName)) {
                schema.addColumn(complexElementName, "TEXT");
            }
        }
    }


    private static boolean isSimpleElement(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }
        return true;
    }

    private static String inferDataType(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "VARCHAR(255)";
        }
        try {
            Integer.parseInt(content);
            return content.length() <= 11 ? "INT" : "BIGINT";
        } catch (NumberFormatException e1) {
            try {
                Double.parseDouble(content);
                return "DECIMAL(10,2)";
            } catch (NumberFormatException e2) {
                if (content.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "DATE";
                }
                return content.length() > 255 ? "TEXT" : "VARCHAR(255)";
            }
        }
    }

    private static void createTables(Map<String, TableSchema> tableSchemas) throws SQLException {
        for (TableSchema schema : tableSchemas.values()) {
            createTable.tableCreation(schema.getTableName(), schema.getColumns());
        }
    }

    private static void displayTableData(String tableName) {
        String query = "SELECT * FROM " + tableName;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                System.out.print(metaData.getColumnName(i) + "\t");
            }
            System.out.println();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving data: " + e.getMessage());
        }
    }

    public static class TableSchema {
        private final String tableName;
        public final String parentTableName;
        private final Map<String, String> columns;

        public TableSchema(String tableName, String parentTableName) {
            this.tableName = tableName;
            this.parentTableName = parentTableName;
            this.columns = new HashMap<>();
        }

        public void addColumn(String columnName, String dataType) {
            columns.put(columnName, dataType);
        }

        public String getTableName() {
            return tableName;
        }

        public Map<String, String> getColumns() {
            return columns;
        }
    }
}