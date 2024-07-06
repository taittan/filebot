package com.infott.filebot.unzip;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

@SuppressWarnings("restriction")
public class UnzipManager {

    private TextField sourceField;
    private TextField targetField;
    private TextField sevenZipField;
    private TextArea logArea;
    private ExecutorService executorService;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public VBox createUnzipUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);
        
        int length = 590;

        // Source file
        Label sourceLabel = new Label("Jar File:");
        GridPane.setConstraints(sourceLabel, 0, 0);

        sourceField = new TextField();
        sourceField.setPrefWidth(length);
        GridPane.setConstraints(sourceField, 1, 0);

        // Enable drag and drop for sourceField
        sourceField.setOnDragOver(event -> {
            if (event.getGestureSource() != sourceField && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        sourceField.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                sourceField.setText(file.getAbsolutePath());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Button sourceButton = new Button("Browse");
        sourceButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR files", "*.jar"));
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                sourceField.setText(selectedFile.getAbsolutePath());
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

        // 7-Zip location
        Label sevenZipLabel = new Label("7-Zip Location:");
        GridPane.setConstraints(sevenZipLabel, 0, 2);

        sevenZipField = new TextField();
        sevenZipField.setText("D:\\Program Files\\7-Zip\\7z.exe");
        sevenZipField.setPrefWidth(length);
        GridPane.setConstraints(sevenZipField, 1, 2);

        // Unzip button
        Button unzipButton = new Button("Unzip");
        unzipButton.setOnAction(e -> executorService.submit(() -> unzipFiles(sevenZipField.getText())));
        GridPane.setConstraints(unzipButton, 1, 3);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(grid, logArea);
        vbox.setPadding(new Insets(10, 10, 10, 10));

        // set Tab order
        sourceField.setOnKeyPressed(event -> handleTabPress(event, targetField));
        targetField.setOnKeyPressed(event -> handleTabPress(event, sevenZipField));
        sevenZipField.setOnKeyPressed(event -> handleTabPress(event, unzipButton));
        unzipButton.setOnKeyPressed(event -> handleTabPress(event, sourceField));
        
        grid.getChildren().addAll(sourceLabel, sourceField, sourceButton, targetLabel, targetField, targetButton, sevenZipLabel, sevenZipField, unzipButton);

        executorService = Executors.newFixedThreadPool(2);

        return vbox;
    }

    public void stop() {
        executorService.shutdown();
    }

    private void unzipFiles(String sevenZipPath) {
        String srcFile = sourceField.getText();
        String tgtFolder = targetField.getText();

        if (srcFile.isEmpty() || tgtFolder.isEmpty() || sevenZipPath.isEmpty()) {
            showAlert("Error", "Please fill in all fields");
            return;
        }

        File targetDir = new File(tgtFolder);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        startTime = LocalDateTime.now();
        log("Unzip start time: " + getCurrentTime(startTime));

        ProcessBuilder processBuilder = new ProcessBuilder(
            sevenZipPath, "x", srcFile, "-o" + tgtFolder, "-y");
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            process.getOutputStream().close();  // Close the output stream to prevent the process from waiting for input

            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    log(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log("Unzip completed successfully.");
            } else {
                log("Unzip failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log("Error: " + e.getMessage());
            e.printStackTrace();
        }

        endTime = LocalDateTime.now();
        log("Unzip end time: " + getCurrentTime(endTime) + ". Time taken: " + getTimeTaken(startTime, endTime));
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            if (logArea.getParagraphs().size() > 10) {
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
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
