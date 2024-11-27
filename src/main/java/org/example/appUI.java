    package org.example;

    import javafx.application.Application;
    import javafx.application.Platform;
    import javafx.collections.FXCollections;
    import javafx.collections.ObservableList;
    import javafx.geometry.Insets;
    import javafx.scene.Scene;
    import javafx.scene.control.ComboBox;
    import javafx.scene.control.TextArea;
    import javafx.scene.control.TextField;
    import javafx.scene.layout.GridPane;
    import javafx.scene.layout.HBox;
    import javafx.scene.layout.VBox;
    import javafx.stage.FileChooser;
    import javafx.stage.Stage;
    import javafx.scene.control.*;
    import org.w3c.dom.*;

    import javax.xml.parsers.DocumentBuilder;
    import javax.xml.parsers.DocumentBuilderFactory;
    import java.io.File;
    import java.sql.*;
    import java.util.ArrayList;
    import java.util.List;


    public class appUI extends Application {
        private File lastDirectory = null;
        private TextField filePathField;
        private ComboBox<String> schemaChoice;
        private TextField tableNameField;
        private TextArea resultArea;
        private TableView<ObservableList<String>> tableView;
        private String lastProcessedTableName;
        public static void main(String[] args)
        {
            launch(args);
        }

        @Override
        public void start(Stage primaryStage)
        {
            primaryStage.setTitle("XML 2 RDBMS");

            VBox mainLayout = new VBox(10);
            mainLayout.setPadding(new Insets(20));

            GridPane filePane = createFileSelectionPane(primaryStage);

            GridPane schemaPane = createSchemaSelectionPane();

            resultArea = new TextArea();
            resultArea.setEditable(false);
            resultArea.setWrapText(true);
            resultArea.setPrefHeight(300);

            tableView = new TableView<>();
            tableView.setPrefHeight(300);

            HBox buttonBox = new HBox(10);
            Button processButton = new Button("Process XML");
            processButton.setOnAction(e -> processXML());

            Button displayButton = new Button("Display Table Data");
            displayButton.setOnAction(e -> {
                String tableName = getLastProcessedTableName();
                if(tableName != null){
                    displayTableData(tableName);
                }else{
                    showAlert("No table data available. Please process an XML file first.");
                }
            });

            buttonBox.getChildren().addAll(processButton, displayButton);

            mainLayout.getChildren().addAll(
                    new Label("XML to RDBMS Converter"),
                    filePane,
                    schemaPane,
                    processButton,
                    displayButton,
                    new Label("Result:"),
                    resultArea,
                    new Label("Table Data:"),
                    tableView
            );

            Scene scene = new Scene(mainLayout, 500, 600);
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        private GridPane createFileSelectionPane(Stage stage)
        {
            GridPane pane = new GridPane();
            pane.setHgap(10);
            pane.setVgap(10);

            filePathField = new TextField();
            filePathField.setPromptText("Select XML file");
            filePathField.setPrefWidth(300);

            Button browseButton = new Button("Browse");
            browseButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select XML file");

                if(lastDirectory != null && lastDirectory.exists()){
                    fileChooser.setInitialDirectory(lastDirectory);
                }

                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
                File selectedFile = fileChooser.showOpenDialog(stage);
                if(selectedFile != null) {
                    filePathField.setText(selectedFile.getAbsolutePath());

                    lastDirectory = selectedFile.getParentFile();
                }
            });

            pane.add(new Label("XML File"), 0, 0);
            pane.add(filePathField, 1, 0);
            pane.add(browseButton, 2, 0);

            return pane;
        }

        private GridPane createSchemaSelectionPane()
        {
            GridPane pane = new GridPane();
            pane.setHgap(10);
            pane.setVgap(10);

            schemaChoice = new ComboBox<>();
            schemaChoice.getItems().addAll("New Schema" , "Existing Schema");
            schemaChoice.setValue("New Schema");

            tableNameField = new TextField();
            tableNameField.setPromptText("Table Name");
            tableNameField.setDisable(true);

            schemaChoice.setOnAction(e -> tableNameField.setDisable(
                    schemaChoice.getValue().equals("New Schema")
            ));

            pane.add(new Label("Schema Type"), 0,0);
            pane.add(schemaChoice, 1, 0);
            pane.add(new Label("Table Name"), 0, 1);
            pane.add(tableNameField, 1, 1);

            return pane;
        }

        private String extractTableName(Document doc) {
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) child;
                    String tableName = null;

                    if (childElement.getParentNode() != null) {
                        tableName = childElement.getParentNode().getNodeName();
                    }

                    if (tableName == null || tableName.trim().isEmpty()) {
                        tableName = childElement.getTagName();
                    }

                    if (tableName == null || tableName.trim().isEmpty()) {
                        tableName = root.getTagName();
                    }

                    return sanitizeTableName(tableName);
                }
            }

            return sanitizeTableName(root.getTagName());
        }

        private void processXML() {
            resultArea.clear();
            tableView.getColumns().clear();
            tableView.getItems().clear();

            if (filePathField.getText().isEmpty()) {
                showAlert("Please select an XML file");
                return;
            }

            try {
                String[] args;
                String tableName;

                if (schemaChoice.getValue().equals("Existing Schema")) {
                    if (tableNameField.getText().isEmpty()) {
                        showAlert("Please enter a table name");
                        return;
                    }
                    args = new String[]{filePathField.getText(), "existing", tableNameField.getText()};
                    tableName = tableNameField.getText();
                } else {
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new File(filePathField.getText()));
                        doc.getDocumentElement().normalize();

                        tableName = extractTableName(doc);
                    } catch (Exception e) {
                        tableName = "xml_table";
                    }

                    args = new String[]{filePathField.getText()};
                }

                if (tableName == null || tableName.trim().isEmpty()) {
                    tableName = "xml_table";
                }

                lastProcessedTableName = tableName;

                System.out.println("Determined Table Name: " + tableName);
                resultArea.appendText("Processing with table name: " + tableName + "\n");

                readXMLfile.main(args);
                resultArea.appendText("XML file processed successfully\n");

            } catch (Exception e) {
                showAlert("Error processing XML file: " + e.getMessage());
                e.printStackTrace();
            }
        }


        private void displayTableData(String tableName) {
            tableName = sanitizeTableName(tableName);

            Connection conn = null;
            try {
                conn = DBConnection.getConnection();

                if (conn == null) {
                    showAlert("Failed to establish database connection");
                    return;
                }

                String query = "SELECT * FROM " + tableName;

                // Prepare lists to store column names and data
                List<String> columnNames = new ArrayList<>();
                List<List<String>> tableData = new ArrayList<>();

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // Capture column names
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(metaData.getColumnName(i));
                    }

                    // Capture data
                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getString(i) != null ? rs.getString(i) : "N/A");
                        }
                        tableData.add(row);
                    }
                }

                // Use Platform.runLater to update JavaFX UI
                Platform.runLater(() -> {
                    // Clear existing columns and data
                    tableView.getColumns().clear();

                    // Create columns
                    for (String columnName : columnNames) {
                        TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
                        column.setCellValueFactory(param ->
                                new javafx.beans.property.SimpleStringProperty(param.getValue().get(
                                        columnNames.indexOf(columnName)
                                ))
                        );
                        column.setPrefWidth(100);
                        tableView.getColumns().add(column);
                    }

                    // Convert data to ObservableList
                    ObservableList<ObservableList<String>> observableData = FXCollections.observableArrayList();
                    for (List<String> row : tableData) {
                        observableData.add(FXCollections.observableArrayList(row));
                    }

                    // Set data to table
                    tableView.setItems(observableData);

                    // Update result area
                    resultArea.appendText("\nTable data retrieved successfully.");
                    resultArea.appendText("\nTotal rows: " + tableData.size());
                });

            } catch (SQLException e) {
                showAlert("Error retrieving table data: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private String sanitizeTableName(String tableName) {
            return tableName.replaceAll("[^a-zA-Z0-9_]", "");
        }

        private void showAlert(String message)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        private String getLastProcessedTableName(){
            return lastProcessedTableName;
        }
    }

