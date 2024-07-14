package com.infott.filebot.sql;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@SuppressWarnings("restriction")
public class CsvGenInsertSQL {

    private TextField csvFilePathField;
    private TextArea selectSqlArea;
    private TextField outputSqlFileField;
    private TextArea logArea;

    public VBox createCsvToInsertSQLUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        int textFieldLength = 631;

        // First row: CSV file path
        Label csvFileLabel = new Label("CSV File:");
        GridPane.setConstraints(csvFileLabel, 0, 0);

        csvFilePathField = new TextField();
        csvFilePathField.setPrefWidth(textFieldLength);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                csvFilePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        csvFilePathField.setOnDragOver(event -> {
            if (event.getGestureSource() != csvFilePathField && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        csvFilePathField.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().endsWith(".csv")) {
                    csvFilePathField.setText(file.getAbsolutePath());
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        HBox csvFileBox = new HBox(5, csvFilePathField, browseButton);
        GridPane.setConstraints(csvFileBox, 1, 0);

        // Second row: Select SQL
        Label selectSqlLabel = new Label("Select SQL:");
        GridPane.setConstraints(selectSqlLabel, 0, 1);

        selectSqlArea = new TextArea();
        selectSqlArea.setPrefWidth(textFieldLength);
        selectSqlArea.setPrefRowCount(8);
        selectSqlArea.setWrapText(true);
        GridPane.setConstraints(selectSqlArea, 1, 1);

        // Third row: Output SQL file path
        Label outputSqlFileLabel = new Label("Insert File:");
        GridPane.setConstraints(outputSqlFileLabel, 0, 2);

        outputSqlFileField = new TextField();
        outputSqlFileField.setPrefWidth(textFieldLength);
        GridPane.setConstraints(outputSqlFileField, 1, 2);

        // Fourth row: Execute button
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> generateInsertSQL());
        GridPane.setConstraints(executeButton, 1, 3);

        // Fifth row: Log area
        logArea = new TextArea();
        logArea.setPrefWidth(textFieldLength);
        logArea.setPrefRowCount(8);
        logArea.setWrapText(true);
        logArea.setEditable(false);
        GridPane.setConstraints(logArea, 0, 4, 3, 1);

        // Add all nodes to the grid
        grid.getChildren().addAll(
                csvFileLabel, csvFileBox,
                selectSqlLabel, selectSqlArea,
                outputSqlFileLabel, outputSqlFileField,
                executeButton,
                logArea
        );

        return new VBox(grid);
    }

    private void generateInsertSQL() {
        String csvFilePath = csvFilePathField.getText().trim();
        String selectSql = selectSqlArea.getText().trim();
        String outputSqlFilePath = outputSqlFileField.getText().trim();

        if (csvFilePath.isEmpty() || selectSql.isEmpty() || outputSqlFilePath.isEmpty()) {
            log("Error: CSV file path, Select SQL, or Output SQL file path is empty");
            return;
        }

        File csvFile = new File(csvFilePath);
        if (!csvFile.exists()) {
            log("Error: CSV file does not exist");
            return;
        }

        try (BufferedReader csvReader = new BufferedReader(new FileReader(csvFile));
             BufferedWriter sqlWriter = new BufferedWriter(new FileWriter(outputSqlFilePath))) {

            String tableName = extractTableName(selectSql);
            if (tableName == null) {
                log("Error: Unable to extract table name from Select SQL");
                return;
            }

            String[] columns = extractColumns(selectSql);
            String[] columnTypes = extractColumnTypes(selectSql);
            if (columns == null || columns.length == 0 || columnTypes == null || columnTypes.length != columns.length) {
                log("Error: Unable to extract columns or column types from Select SQL");
                return;
            }

            String headerLine = csvReader.readLine();
            if (headerLine == null) {
                log("Error: CSV file is empty");
                return;
            }

            String[] csvColumns = headerLine.split(",");
            if (csvColumns.length != columns.length) {
                log("Error: Column count in CSV file does not match column count in Select SQL");
                return;
            }

            String line;
            int count = 0;
            long startTime = System.currentTimeMillis();

            while ((line = csvReader.readLine()) != null) {
                String[] values = line.split(",");
                StringJoiner valueJoiner = new StringJoiner(", ", "(", ")");

                for (int i = 0; i < values.length; i++) {
                    String value = trimDoubleQuote(values[i]);
                    if (columnTypes[i].startsWith("TIMESTAMP")) {
                        valueJoiner.add("TO_TIMESTAMP('" + value + "', 'yyyy/mm/dd hh24:mi:ss.ff3')");
                    } else {
                        valueJoiner.add("'" + value + "'");
                    }
                }

                String insertSql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES " + valueJoiner.toString() + ";";
                sqlWriter.write(insertSql);
                sqlWriter.newLine();
                count++;
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log("Generated " + count + " INSERT statements");
            log("Execution completed in " + duration + " ms");

        } catch (IOException e) {
            log("Error processing file: " + e.getMessage());
        }
    }

    private String extractTableName(String selectSql) {
        selectSql = selectSql.toLowerCase();
        int fromIndex = selectSql.indexOf(" from ");
        if (fromIndex == -1) {
            return null;
        }
        return selectSql.substring(fromIndex + 6).trim();
    }

    private String[] extractColumns(String sql) {
        String lowerCaseSql = sql.toLowerCase();
        int selectIndex = lowerCaseSql.indexOf("select") + 6;
        int fromIndex = lowerCaseSql.indexOf("from");

        String columnsPart = sql.substring(selectIndex, fromIndex).trim();

        List<String> columns = new ArrayList<>();
        StringBuilder column = new StringBuilder();
        int bracketLevel = 0;

        for (char ch : columnsPart.toCharArray()) {
            if (ch == ',' && bracketLevel == 0) {
                columns.add(column.toString().trim());
                column.setLength(0);
            } else {
                if (ch == '(') bracketLevel++;
                if (ch == ')') bracketLevel--;
                column.append(ch);
            }
        }

        if (column.length() > 0) {
            columns.add(column.toString().trim());
        }

        List<String> finalColumns = new ArrayList<>();
        for (String col : columns) {
            String[] parts = col.split("\\s+as\\s+", 2);
            if (parts.length == 2) {
                finalColumns.add(parts[1].trim());
            } else {
                finalColumns.add(parts[0].trim());
            }
        }

        return finalColumns.toArray(new String[0]);
    }

    private String[] extractColumnTypes(String sql) {
        String lowerCaseSql = sql.toLowerCase();
        int selectIndex = lowerCaseSql.indexOf("select") + 6;
        int fromIndex = lowerCaseSql.indexOf("from");

        String columnsPart = sql.substring(selectIndex, fromIndex).trim();

        List<String> columnTypes = new ArrayList<>();
        StringBuilder column = new StringBuilder();
        int bracketLevel = 0;

        for (char ch : columnsPart.toCharArray()) {
            if (ch == ',' && bracketLevel == 0) {
                columnTypes.add(getColumnType(column.toString().trim()));
                column.setLength(0);
            } else {
                if (ch == '(') bracketLevel++;
                if (ch == ')') bracketLevel--;
                column.append(ch);
            }
        }

        if (column.length() > 0) {
            columnTypes.add(getColumnType(column.toString().trim()));
        }

        return columnTypes.toArray(new String[0]);
    }

    private String getColumnType(String column) {
        if (column.toUpperCase().contains("TO_CHAR")) {
            return "TIMESTAMP";
        } else {
            return "VARCHAR2";
        }
    }
    
    private String trimDoubleQuote(String field) {
    	if (field == null || field.length() < 2) {
    		return field;
    	}
    	
    	if (field.startsWith("\"") && field.endsWith("\"")) {
    		return field.substring(1, field.length() - 1);
    	}
    	
    	return field;
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
}
