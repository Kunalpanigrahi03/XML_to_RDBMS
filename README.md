
# XML_to_RDBMS

The **XML_to_RDBMS** is a Java-based tool designed to automate the process of converting XML files into relational database schemas and loading data into these schemas. By analyzing the XML structure, the application dynamically creates database tables and relationships, making it a powerful solution for developers, database administrators, and organizations dealing with structured data migration or integration.




## Features


- Schema Generation from XML :

Automatically analyzes the structure of the XML file.

Generates relational database tables based on the hierarchy of the XML.

Dynamically infers data types (e.g., VARCHAR, INT, DATE, etc.) for table columns based on XML content.

- Load Data into Existing Schema :

Supports loading XML data into an already defined database schema.

- Recursive XML Parsing :

Handles complex XML structures, including nested elements.

Establishes parent-child relationships between database tables when necessary.

- Validation Mechanisms :
Validates XML file type, extension, and structure before processing.

Displays clear error messages for invalid files.

- Efficient Performance :
Designed to handle XML files efficiently.

Uses optimized recursive parsing and database operations to enhance performance.



## Demo

**New Schema**

https://github.com/user-attachments/assets/9cbe9eac-0757-4211-87b9-bc09ab81d1d9


**Existing Schema**

https://github.com/user-attachments/assets/3178305a-8e31-48eb-a61d-68720a823f71



## Dependencies

- JavaFX Modules (Version 17.0.2)
- MySQL Connector for Java (Version 8.0.33)
- JUnit 5 (Version 5.8.1)
- DOM for XML Parsing (org.w3c.dom:dom)


## Installation

- **Prerequisites**
  
  - Install JDK 17 or later

    Verify using
    
    ```
    java -version
    ```

  - Install Gradle for building the project

    Verify using
    ```
     ./gradlew --version
    ```

- **Steps to Install and Run**

  - Clone the Repository : 
    
    ```bash
    git clone https://github.com/Kunalpanigrahi03/XML_to_RDBMS.git  
    cd XML_to_RDBMS
    ```
  - Update Database Configuration :
    
    Open the DBConnection class (src/main/java/org/example/       DBConnection.java).

    Update the following details :
    ```
    String url = "jdbc:mysql://localhost:3306/your_database_name?useSSL=false";
    String user = "your_username";
    String password = "your_password";
    ```

  - Build the Project : 
    
    ```
    ./gradlew build
    ```
  
  - Launch the Application :
    
     - In IntelliJ IDEA : 

    Go to Run > Edit Configurations

    In the VM Options field, add the following:

    ```
    --module-path "path/to/javafx-sdk-17.0.2/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics
    ```

    Click Apply and OK

    Then run the application by clicking the Run button


     - Using Command Line : 

    Set an enviroment variable for the JavaFX SDK :
    ```
    set JAVA_FX_PATH=C:/path/to/javafx-sdk-17.0.2/lib
    ```

    Now, use this enviroment variable in your command : 
    ```
    java --module-path "%JAVA_FX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp build/classes/java/main org.example.appUI
    ``` 
  
    
