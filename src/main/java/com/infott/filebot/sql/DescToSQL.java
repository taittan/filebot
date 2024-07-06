package com.infott.filebot.sql;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("restriction")
public class DescToSQL {

    private TextField tableNameField;
    private TextArea descOutputArea;
    private TextArea selectOutputArea;

    public VBox createDescToSQLUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        int textAreaWidth = 685;

        // First row: Table name input
        Label tableNameLabel = new Label("Table:");
        GridPane.setConstraints(tableNameLabel, 0, 0);

        tableNameField = new TextField();
        tableNameField.setPrefWidth(textAreaWidth);
        GridPane.setConstraints(tableNameField, 1, 0);

        // Second row: Desc output
        Label descLabel = new Label("Desc output:");
        GridPane.setConstraints(descLabel, 0, 1);

        descOutputArea = new TextArea();
        descOutputArea.setPrefWidth(textAreaWidth);
        descOutputArea.setPrefRowCount(10);
        descOutputArea.setWrapText(true);
        GridPane.setConstraints(descOutputArea, 1, 1);

        // Third row: Execute button
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> generateSelectSQL());
        GridPane.setConstraints(executeButton, 1, 2);

        // Fourth row: Select SQL output
        Label selectLabel = new Label("Select SQL:");
        GridPane.setConstraints(selectLabel, 0, 3);

        selectOutputArea = new TextArea();
        selectOutputArea.setPrefWidth(textAreaWidth);
        selectOutputArea.setPrefRowCount(8);
        selectOutputArea.setWrapText(true);
        GridPane.setConstraints(selectOutputArea, 1, 3);

        // Add all nodes to the grid
        grid.getChildren().addAll(
                tableNameLabel, tableNameField,
                descLabel, descOutputArea,
                executeButton,
                selectLabel, selectOutputArea
        );

        // Set Tab order
        tableNameField.setOnKeyPressed(event -> handleTabPress(event, descOutputArea));
        descOutputArea.setOnKeyPressed(event -> handleTabPress(event, executeButton));
        executeButton.setOnKeyPressed(event -> handleTabPress(event, selectOutputArea));
        selectOutputArea.setOnKeyPressed(event -> handleTabPress(event, tableNameField));

        return new VBox(grid);
    }

    private void generateSelectSQL() {
        String tableName = tableNameField.getText().trim();
        String descOutput = descOutputArea.getText().trim();
        if (tableName.isEmpty()) {
            log("Table name is empty");
            return;
        }
        if (descOutput.isEmpty()) {
            log("DESC output is empty");
            return;
        }

        String[] lines = descOutput.split("\n");
        if (lines.length < 3) {
            log("Invalid DESC output format");
            return;
        }

        List<String> columns = new ArrayList<>();
        for (int i = 2; i < lines.length; i++) {
        	String line = lines[i].trim().replace("NOT NULL", "NOT_NULL");
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3) {
                continue;
            }
            String columnName = parts[0].toLowerCase();
            String columnType = parts[2].toUpperCase();
            if (columnType.startsWith("TIMESTAMP")) {
                columns.add("to_char(" + columnName + ", 'yyyy/mm/dd hh24:mi:ss.ff3') as " + columnName);
            } else {
                columns.add(columnName);
            }
        }

        if (columns.isEmpty()) {
            log("No valid columns found in DESC output");
            return;
        }

        String selectSQL = "select " + String.join(", ", columns) + " from " + tableName;
        selectOutputArea.setText(selectSQL);
    }

    private void handleTabPress(KeyEvent event, Node nextNode) {
        if (event.getCode() == KeyCode.TAB) {
            nextNode.requestFocus();
            event.consume();
        }
    }

    private void log(String message) {
        Platform.runLater(() -> {
            selectOutputArea.appendText(message + "\n");
            if (selectOutputArea.getParagraphs().size() > 10) {
                selectOutputArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
}
