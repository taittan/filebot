package com.infott.filebot.sql;

import javafx.application.Platform;
import javafx.geometry.Insets;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("restriction")
public class SQLInserter {

    private TextField filePathField;
    private ComboBox<DatabaseConfig> databaseComboBox;
    private TextArea logArea;
    private ExecutorService executorService;

    public VBox createSQLInserterUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        int textFieldLength = 635;

        // First row: SQL File path
        Label filePathLabel = new Label("SQL File:");
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
                if (file.getName().endsWith(".sql")) {
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
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL files", "*.sql"));
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });
        GridPane.setConstraints(browseButton, 2, 0);

        // Second row: Database selection
        Label databaseLabel = new Label("Database:");
        GridPane.setConstraints(databaseLabel, 0, 1);

        databaseComboBox = new ComboBox<>();
        databaseComboBox.getItems().addAll(DatabaseConfig.values());
        databaseComboBox.setValue(DatabaseConfig.MYSQL);
        GridPane.setConstraints(databaseComboBox, 1, 1);

        // Third row: Execute and Clear Log buttons
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> executeSQLFile());
        Label emptyLabel = new Label();
        emptyLabel.setPrefWidth(400);
        Button clearButton = new Button("Clear Log");
        clearButton.setOnAction(e -> logArea.clear());

        HBox buttonBox = new HBox(10, executeButton, emptyLabel, clearButton);
        GridPane.setConstraints(buttonBox, 1, 2, 2, 1);

        // Fourth row: Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox logVBox = new VBox(logArea);
        GridPane.setConstraints(logVBox, 0, 3, 3, 1);

        // Add all nodes to the grid
        grid.getChildren().addAll(
                filePathLabel, filePathField, browseButton,
                databaseLabel, databaseComboBox,
                buttonBox,
                logVBox
        );

        // Set Tab order
        filePathField.setOnKeyPressed(event -> handleTabPress(event, databaseComboBox));
        databaseComboBox.setOnKeyPressed(event -> handleTabPress(event, executeButton));
        executeButton.setOnKeyPressed(event -> handleTabPress(event, clearButton));
        clearButton.setOnKeyPressed(event -> handleTabPress(event, filePathField));

        executorService = Executors.newFixedThreadPool(2);

        return new VBox(grid);
    }

    private void executeSQLFile() {
        executorService.submit(() -> {
            String filePath = filePathField.getText().trim();
            DatabaseConfig databaseConfig = databaseComboBox.getValue();

            if (filePath.isEmpty()) {
                log("Error: File path is empty");
                return;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                log("Error: File does not exist");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                List<String> statements = new ArrayList<>();
                LocalDateTime startTime = LocalDateTime.now();

                while ((line = reader.readLine()) != null) {
                    if (line.trim().toUpperCase().startsWith("INSERT")) {
                        if (line.endsWith(";")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        statements.add(line);
                    }
                    if (statements.size() == 500) {
                        if (!executeStatements(statements, databaseConfig)) {
                            break;
                        }
                        statements.clear();
                    }
                }
                if (!statements.isEmpty()) {
                    executeStatements(statements, databaseConfig);
                }

                LocalDateTime endTime = LocalDateTime.now();
                log("Execution completed. " + statements.size() + " statements executed.");
                log("Total time taken: " + getTimeTaken(startTime, endTime));
            } catch (IOException e) {
                log("Error processing file: " + e.getMessage());
            }
        });
    }

    private boolean executeStatements(List<String> statements, DatabaseConfig databaseConfig) {
        try {
            Class.forName(databaseConfig.getDriverClassName());
            try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
                 Statement statement = connection.createStatement()) {

                // Set NLS format for the session
                statement.execute("ALTER SESSION SET NLS_DATE_LANGUAGE = 'ENGLISH'");
                connection.setAutoCommit(false);

                for (String sql : statements) {
                    try {
                        statement.execute(sql);
                        log("Executed successfully at " + getCurrentTime(LocalDateTime.now()));
                    } catch (SQLException e) {
                        log("SQL Error: " + e.getMessage() + " for statement: " + sql);
                        connection.rollback();
                        return false;
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                log("SQL Error: " + e.getMessage());
                return false;
            }
        } catch (ClassNotFoundException e) {
            log("Driver not found: " + e.getMessage());
            return false;
        }

        return true;
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

    private String getCurrentTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String getTimeTaken(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        return String.format("%d minutes and %d seconds", minutes, seconds);
    }
}
