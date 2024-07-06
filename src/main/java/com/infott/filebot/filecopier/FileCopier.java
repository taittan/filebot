package com.infott.filebot.filecopier;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

@SuppressWarnings("restriction")
public class FileCopier {

    private TextField sourceField;
    private TextField targetField;
    private TextField extensionsField;
    private TextArea logArea;
    private ExecutorService executorService;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public VBox createFileCopierUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);
        
        int length = 590;

        // Source folder
        Label sourceLabel = new Label("Source Folder:");
        GridPane.setConstraints(sourceLabel, 0, 0);

        sourceField = new TextField();
        sourceField.setPrefWidth(length);
        GridPane.setConstraints(sourceField, 1, 0);

        Button sourceButton = new Button("Browse");
        sourceButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(null);
            if (selectedDirectory != null) {
                sourceField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        GridPane.setConstraints(sourceButton, 2, 0);

        // Target folder
        Label targetLabel = new Label("Target Folder:");
        GridPane.setConstraints(targetLabel, 0, 1);

        targetField = new TextField();
        targetField.setPrefWidth(length);
        GridPane.setConstraints(targetField, 1, 1);

        Button targetButton = new Button("Browse");
        targetButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(null);
            if (selectedDirectory != null) {
                targetField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        GridPane.setConstraints(targetButton, 2, 1);

        // File extensions
        Label extensionsLabel = new Label("File Ext.:");
        GridPane.setConstraints(extensionsLabel, 0, 2);

        extensionsField = new TextField();
        extensionsField.setPrefWidth(length);
        extensionsField.setText("java,properties,xml,yml,bmp,css");
        GridPane.setConstraints(extensionsField, 1, 2);

        // Copy button
        Button copyButton = new Button("Copy Files");
        copyButton.setOnAction(e -> executorService.submit(this::copyFiles));
        
        // Clear All button
        Button clearAllButton = new Button("Clear All");
        clearAllButton.setPrefWidth(80);
        clearAllButton.setOnAction(e -> clearFields());
        
        // Clear Log button
        Button clearLogButton = new Button("Clear Log");
        clearLogButton.setOnAction(e -> logArea.clear());

        // HBox for buttons
        HBox hbox = new HBox(10, copyButton, clearAllButton, clearLogButton);
        GridPane.setConstraints(hbox, 1, 3);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(grid, logArea);
        vbox.setPadding(new Insets(10, 10, 10, 10));

        // set Tab order
        sourceField.setOnKeyPressed(event -> handleTabPress(event, targetField));
        targetField.setOnKeyPressed(event -> handleTabPress(event, extensionsField));
        extensionsField.setOnKeyPressed(event -> handleTabPress(event, copyButton));
        copyButton.setOnKeyPressed(event -> handleTabPress(event, sourceField));
        
        grid.getChildren().addAll(sourceLabel, sourceField, sourceButton, targetLabel, targetField, targetButton, extensionsLabel, extensionsField, hbox);

        executorService = Executors.newFixedThreadPool(2);

        return vbox;
    }

    public void stop() {
        executorService.shutdown();
    }

    private void copyFiles() {
        String srcFolder = sourceField.getText();
        String tgtFolder = targetField.getText();
        String[] extensions = extensionsField.getText().split(",");

        if (srcFolder.isEmpty() || tgtFolder.isEmpty() || extensions.length == 0) {
            showAlert("Error", "Please fill in all fields");
            return;
        }

        startTime = LocalDateTime.now();
        log("Copy start time: " + getCurrentTime(startTime));

        Path srcPath = Paths.get(srcFolder);
        Path tgtPath = Paths.get(tgtFolder);

        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = tgtPath.resolve(srcPath.relativize(dir));
                    if (Files.notExists(targetDir)) {
                        Files.createDirectories(targetDir);
                        Files.setLastModifiedTime(targetDir, Files.getLastModifiedTime(dir));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    for (String ext : extensions) {
                        if (file.toString().endsWith(ext.trim())) {
                            Path relativePath = srcPath.relativize(file);
                            Path targetPath = tgtPath.resolve(relativePath);
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(file, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                            log("Copied: " + file.toString() + " to " + targetPath.toString());
                            break;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            log("Error: " + e.getMessage());
        }

        endTime = LocalDateTime.now();
        log("Copy end time: " + getCurrentTime(endTime) + ". Time taken: " + getTimeTaken(startTime, endTime));
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            if (logArea.getParagraphs().size() > 10) {
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    private void clearFields() {
        sourceField.clear();
        targetField.clear();
        extensionsField.clear();
        logArea.clear();
    }
    
    private void handleTabPress(KeyEvent event, Node nextNode) {
        if (event.getCode() == KeyCode.TAB) {
            nextNode.requestFocus();
            event.consume();
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
