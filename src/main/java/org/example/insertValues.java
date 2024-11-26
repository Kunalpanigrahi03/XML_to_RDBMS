package org.example;

import org.w3c.dom.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.sql.*;
import java.util.*;

public class insertValues {
    public static void insertRecords(Element rootElement, Map<String, readXMLfile.TableSchema> tableSchemas) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String rootId = UUID.randomUUID().toString();

            insertRootRecord(rootElement, tableSchemas, connection, rootId);

            NodeList children = rootElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    processNestedElement((Element) child, rootId, tableSchemas, connection);
                }
            }
        }
    }

    private static void insertRootRecord(Element rootElement, Map<String, readXMLfile.TableSchema> tableSchemas, Connection connection, String rootId) throws SQLException {
        String tableName = rootElement.getNodeName();
        readXMLfile.TableSchema schema = tableSchemas.get(tableName);

        List<String> columns = new ArrayList<>(schema.getColumns().keySet());
        List<Object> values = new ArrayList<>(Collections.nCopies(columns.size(), null));

        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);

            if (columnName.equals("id")) {
                values.set(i, rootId);
            } else {
                NodeList children = rootElement.getElementsByTagName(columnName);
                if (children.getLength() > 0) {
                    Element child = (Element) children.item(0);
                    values.set(i, serializeChildElements(child));
                }
            }
        }

        String sql = buildInsertSQL(tableName, columns);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.executeUpdate();
        }
    }

    private static void processNestedElement(Element element, String parentId, Map<String, readXMLfile.TableSchema> tableSchemas, Connection connection) throws SQLException {
        String tableName = element.getNodeName();
        String currentId = element.getAttribute("id");
        if (currentId.isEmpty()) {
            NodeList idNodes = element.getElementsByTagName("id");
            if (idNodes.getLength() > 0) {
                currentId = idNodes.item(0).getTextContent();
            }
        }

        if (currentId.isEmpty()) {
            currentId = UUID.randomUUID().toString();
        }

        readXMLfile.TableSchema schema = tableSchemas.get(tableName);
        List<String> columns = new ArrayList<>(Arrays.asList("id", "parent_id"));
        List<Object> values = new ArrayList<>(Arrays.asList(currentId, parentId));

        // Handle attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String columnName = attr.getNodeName();
            if (columnName.equals("id")) {
                continue;
            }
            columns.add(columnName);
            values.add(attr.getNodeValue());
        }

        // Track nested complex elements for serialization
        Map<String, List<Element>> complexElementGroups = new HashMap<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                if (isSimpleElement(childElement)) {
                    String columnName = childElement.getNodeName();
                    if (!columns.contains(columnName)) {
                        columns.add(columnName);
                        values.add(childElement.getTextContent());
                    }
                } else {
                    // Collect complex elements by their tag name
                    String complexElementName = childElement.getNodeName();
                    complexElementGroups.computeIfAbsent(complexElementName, k -> new ArrayList<>()).add(childElement);
                    processNestedElement(childElement, currentId, tableSchemas, connection);
                }
            }
        }

        for (Map.Entry<String, List<Element>> entry : complexElementGroups.entrySet()) {
            String complexElementName = entry.getKey();
            List<Element> complexElements = entry.getValue();

            String serializedComplexElements = serializeComplexElements(complexElements);

            if (!complexElementName.equals(tableName) && schema.parentTableName != null) {
                columns.add(complexElementName);
                values.add(serializedComplexElements);
            }
        }

        String sql = buildInsertSQL(tableName, columns);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.executeUpdate();
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

    private static String serializeComplexElements(List<Element> complexElements) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            for (Element element : complexElements) {
                transformer.transform(new DOMSource(element), new StreamResult(writer));
            }
            return writer.toString().replaceAll(">\\s+<", "><").trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String buildInsertSQL(String tableName, List<String> columns) {
        String columnNames = String.join(", ", columns);
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnNames, placeholders);
    }

    private static String serializeChildElements(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");


            StringWriter writer = new StringWriter();

            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    transformer.transform(new DOMSource(child), new StreamResult(writer));
                }
            }
            return writer.toString().replaceAll(">\\s+<", "><").trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}