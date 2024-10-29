package org.example;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class readXMLfile {
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, SQLException {
        DBConnection.connectDB();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse("C:\\Users\\KIIT\\OneDrive\\Desktop\\DomParser\\data\\customers.xml");
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        String tableName = root.getTagName();

        NodeList children = root.getChildNodes();
        String recordTag = null;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                recordTag = children.item(i).getNodeName();
                break;
            }
        }

        if (recordTag == null) {
            System.out.println("No record elements found");
            return;
        }

        NodeList records = root.getElementsByTagName(recordTag);
        if (records.getLength() == 0) {
            System.out.println("No records found");
            return;
        }

        Map<String, String> columns = new HashMap<>();
        collectAllColumns(records, recordTag, columns);

        createTable.createTable(tableName, recordTag, columns);
        insertValues.insertAllRecords(tableName, recordTag, records, columns);

        DBConnection.closeDB();
    }

    private static void collectAllColumns(NodeList records, String recordTag, Map<String, String> columns) {
        columns.put(recordTag, "VARCHAR(255)");

        for (int i = 0; i < records.getLength(); i++) {
            Element record = (Element) records.item(i);
            NodeList children = record.getChildNodes();

            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    String columnName = child.getTagName();

                    if (!columns.containsKey(columnName)) {
                        columns.put(columnName, inferDataType(child.getTextContent()));
                    } else if (columns.get(columnName).equals("VARCHAR(255)")) {
                        String newType = inferDataType(child.getTextContent());
                        if (!newType.equals("VARCHAR(255)")) {
                            columns.put(columnName, newType);
                        }
                    }
                }
            }

            NamedNodeMap attributes = record.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Node attr = attributes.item(j);
                String columnName = "attr_" + attr.getNodeName();
                if (!columns.containsKey(columnName)) {
                    columns.put(columnName, inferDataType(attr.getNodeValue()));
                }
            }
        }
    }

    private static String inferDataType(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "VARCHAR(255)";
        }
        try {
            Integer.parseInt(content);
            return "INT";
        } catch (NumberFormatException e1) {
            try {
                Double.parseDouble(content);
                return "DECIMAL(10,2)";
            } catch (NumberFormatException e2) {
                return "VARCHAR(255)";
            }
        }
    }
}