package com.infott.filebot.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

@SuppressWarnings("restriction")
public class CsvHandler {

    private TextField filePathField;
    private ComboBox<String> operationTypeComboBox;
    private ComboBox<String> modifyTypeComboBox;
    private TextField replaceField;
    private VBox replaceLabelVBox;
    private TextField columnIndexField;
    private TextField newColumnContentField;
    private Label newColumnContentLabel;
    private TextArea logArea;
    private ExecutorService executorService;

    public VBox createCsvHandlerUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        int textFieldLength = 630;

        // First row: File path
        Label filePathLabel = new Label("CSV File:");
        GridPane.setConstraints(filePathLabel, 0, 0);

        filePathField = new TextField();
        filePathField.setPrefWidth(textFieldLength);
        GridPane.setConstraints(filePathField, 1, 0);

        filePathField.setOnDragOver(event -> {
            if (event.getGestureSource() != filePathField && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        filePathField.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().endsWith(".csv")) {
                    filePathField.setText(file.getAbsolutePath());
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });
        GridPane.setConstraints(browseButton, 2, 0);

        // Second row: Operation type
        Label operationLabel = new Label("Operation:");
        GridPane.setConstraints(operationLabel, 0, 1);

        operationTypeComboBox = new ComboBox<>();
        operationTypeComboBox.getItems().addAll("Add Column", "Delete Column", "Modify Column");
        operationTypeComboBox.setValue("Add Column");
        operationTypeComboBox.setOnAction(e -> {
            updateUIForOperation();
            updateUIForOperationModify();
        });

        modifyTypeComboBox = new ComboBox<>();
        modifyTypeComboBox.getItems().addAll("Replace", "Prefix", "Append");
        modifyTypeComboBox.setValue("Replace");
        modifyTypeComboBox.setVisible(false);
        modifyTypeComboBox.setOnAction(e -> {
            updateUIForModifyType();
        });

        Label replaceLabel = new Label("Replace:");
        replaceLabelVBox = new VBox(5, replaceLabel);
        replaceLabelVBox.setAlignment(Pos.CENTER_LEFT);
        replaceLabelVBox.setVisible(false);

        replaceField = new TextField();
        replaceField.setPrefWidth(220);
        replaceField.setVisible(false);

        HBox comboHBox = new HBox(10, operationTypeComboBox, modifyTypeComboBox, replaceLabelVBox, replaceField);
        GridPane.setConstraints(comboHBox, 1, 1);

        // Third row: Column number and New column content
        Label columnIndexLabel = new Label("Column Index:");
        columnIndexField = new TextField();
        columnIndexField.setPrefWidth(95);

        newColumnContentLabel = new Label("New Column Content:");
        newColumnContentField = new TextField();
        newColumnContentField.setPrefWidth(220);

        VBox columnVBox = new VBox(5, columnIndexLabel, columnIndexField);
        columnVBox.setAlignment(Pos.BOTTOM_LEFT);

        VBox newColumnVBox = new VBox(5, newColumnContentLabel, newColumnContentField);
        newColumnVBox.setAlignment(Pos.BOTTOM_LEFT);

        HBox columnHBox = new HBox(10, columnVBox, newColumnVBox);
        GridPane.setConstraints(columnHBox, 1, 2);

        // Fourth row: Execute button, empty label, and Clear button
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> executeCsvOperation());
        Label emptyLabel = new Label();
        emptyLabel.setPrefWidth(400);
        Button clearButton = new Button("Clear Log");
        clearButton.setOnAction(e -> logArea.clear());

        HBox buttonBox = new HBox(10, executeButton, emptyLabel, clearButton);
        GridPane.setConstraints(buttonBox, 1, 3, 2, 1);

        // Fifth row: Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox logVBox = new VBox(logArea);
        GridPane.setConstraints(logVBox, 0, 4, 3, 1);

        // Add all nodes to the grid
        grid.getChildren().addAll(
                filePathLabel, filePathField, browseButton,
                operationLabel, comboHBox,
                columnHBox,
                buttonBox,
                logVBox
        );

        // Set Tab order
        filePathField.setOnKeyPressed(event -> handleTabPress(event, operationTypeComboBox));
        operationTypeComboBox.setOnKeyPressed(event -> handleTabPress(event, columnIndexField));
        columnIndexField.setOnKeyPressed(event -> handleTabPress(event, newColumnContentField));
        newColumnContentField.setOnKeyPressed(event -> handleTabPress(event, executeButton));
        executeButton.setOnKeyPressed(event -> handleTabPress(event, clearButton));
        clearButton.setOnKeyPressed(event -> handleTabPress(event, filePathField));

        executorService = Executors.newFixedThreadPool(2);

        updateUIForOperation();

        return new VBox(grid);
    }

    private void updateUIForOperation() {
        String operationType = operationTypeComboBox.getValue();
        boolean showNewColumn = operationType.equals("Add Column") || operationType.equals("Modify Column");

        newColumnContentLabel.setVisible(showNewColumn);
        newColumnContentField.setVisible(showNewColumn);
        
        if(operationType.equals("Modify Column")) {
        	newColumnContentLabel.setText("with:");
        } else {
        	newColumnContentLabel.setText("New Column Content:");
        }
    }

    private void updateUIForOperationModify() {
        String operationType = operationTypeComboBox.getValue();
        boolean showModifyOpt = operationType.equals("Modify Column");

        modifyTypeComboBox.setVisible(showModifyOpt);
        replaceLabelVBox.setVisible(showModifyOpt);
        replaceField.setVisible(showModifyOpt);

        if (showModifyOpt) {
            updateUIForModifyType();
        }
    }

    private void updateUIForModifyType() {
        String type = modifyTypeComboBox.getValue();
        boolean show = type.equals("Replace");

        replaceLabelVBox.setVisible(show);
        replaceField.setVisible(show);
        
        if(show) {
        	newColumnContentLabel.setText("with:");
        } else {
        	newColumnContentLabel.setText("New Column Content:");
        }
    }

    private void executeCsvOperation() {
        executorService.submit(() -> {
            String filePath = filePathField.getText().trim();
            String operation = operationTypeComboBox.getValue();
            String columnStr = columnIndexField.getText().trim();
            String newColumnContent = newColumnContentField.getText().trim();
            String replaceContent = replaceField.getText().trim();

            if (filePath.isEmpty() || columnStr.isEmpty()) {
                log("Error: File path or column is empty");
                return;
            }

            int column;
            try {
                column = Integer.parseInt(columnStr) - 1; // Convert from 1-based to 0-based index
            } catch (NumberFormatException e) {
                log("Error: Invalid column number");
                return;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                log("Error: File does not exist");
                return;
            }

            try {
                List<String> lines = Files.readAllLines(file.toPath());
                List<String> updatedLines = processCsv(lines, operation, column, newColumnContent, replaceContent);

                Files.write(file.toPath(), updatedLines);
                log("Operation " + operation + " on column " + (column + 1) + " completed successfully.");
            } catch (IOException e) {
                log("Error processing file: " + e.getMessage());
            }
        });
    }

    private List<String> processCsv(List<String> lines, String operation, int column, String newColumnContent, String replaceContent) {
        List<String> updatedLines = new ArrayList<>();

        String modifyOption = "";
        if ("Modify Column".equals(operation)) {
            modifyOption = modifyTypeComboBox.getValue();
        } else {
            modifyOption = "";
        }

        for (String line : lines) {
            String[] fields = line.split(",");
            StringBuilder updatedLine = new StringBuilder();

            if (operation.equals("Add Column")) {
                for (int i = 0; i < fields.length; i++) {
                    if (i == column) {
                        updatedLine.append(newColumnContent).append(",").append(fields[i]).append(",");
                    } else {
                        updatedLine.append(fields[i]).append(",");
                    }
                }
                if (column >= fields.length) {
                    updatedLine.append(newColumnContent).append(",");
                }
            } else {
                for (int i = 0; i < fields.length; i++) {
                    if (i == column) {
                        switch (operation) {
                            case "Delete Column":
                                // Skip the current column
                                break;
                            case "Modify Column":
                                if ("Replace".equals(modifyOption)) {
                                    if (!replaceContent.isEmpty() && fields[i].contains(replaceContent)) {
                                        fields[i] = fields[i].replace(replaceContent, newColumnContent);
                                    }
                                    updatedLine.append(fields[i]).append(",");
                                } else if ("Prefix".equals(modifyOption)) {
                                    updatedLine.append(newColumnContent).append(fields[i]).append(",");
                                } else if ("Append".equals(modifyOption)) {
                                    updatedLine.append(fields[i]).append(newColumnContent).append(",");
                                }
                                break;
                            default:
                                updatedLine.append(fields[i]).append(",");
                                break;
                        }
                    } else {
                        updatedLine.append(fields[i]).append(",");
                    }
                }
            }

            // Remove the trailing comma
            if (updatedLine.length() > 0 && updatedLine.charAt(updatedLine.length() - 1) == ',') {
                updatedLine.deleteCharAt(updatedLine.length() - 1);
            }

            updatedLines.add(updatedLine.toString());
        }

        return updatedLines;
    }

    private void handleTabPress(KeyEvent event, Node nextNode) {
        if (event.getCode() == KeyCode.TAB) {
            nextNode.requestFocus();
            event.consume();
        }
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            if (logArea.getParagraphs().size() > 10) {
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }
}
