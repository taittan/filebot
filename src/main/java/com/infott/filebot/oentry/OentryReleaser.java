package com.infott.filebot.oentry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

@SuppressWarnings("restriction")
public class OentryReleaser {

    private TextField crField;
    private TextField crPathField;
    private TextArea multiLineTextArea;
    private TextArea logArea;
    private ExecutorService executorService;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private static String crRootPath = "D:\\virtualVM\\P\\ReleaseDoc\\CO Details yyyy";
    private static String tkoRealOentry = "D:\\virtualVM\\HKW20158319C01\\OESUserCFS\\UserMaint\\cfg\\Oentry.cfg";
    private static String skmRealOentry = "D:\\virtualVM\\HKW20158320C01\\OESUserCFS\\UserMaint\\cfg\\Oentry.cfg";

    public VBox createOentryReleaserUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        int textFieldLength = 650;

        // First row: Hardcoded file path
        Label filePathLabel = new Label("Oentry:");
        GridPane.setConstraints(filePathLabel, 0, 0);

        TextField filePathField = new TextField("HKW20158319C01\\\\OESUserCFS\\\\UserMaint\\\\cfg\\\\Oentry.cfg, HKW20158319C01\\\\OESUserCFS\\\\UserMaint\\\\cfg\\\\Oentry.cfg");
        filePathField.setEditable(false);
        filePathField.setPrefWidth(textFieldLength);
        GridPane.setConstraints(filePathField, 1, 0);

        // Second row: CR Label, TextField and Guess button
        Label crLabel = new Label("CR#:");
        GridPane.setConstraints(crLabel, 0, 1);

        crField = new TextField();
        crField.setPrefWidth(100);
        GridPane.setConstraints(crField, 1, 1);

        Button guessButton = new Button("Guess");
        guessButton.setOnAction(e -> guessCrPath());
        GridPane.setConstraints(guessButton, 2, 1);

        // Third row: CR Path Label and TextField
        Label crPathLabel = new Label("CR Path:");
        GridPane.setConstraints(crPathLabel, 0, 2);

        crPathField = new TextField();
        crPathField.setPrefWidth(textFieldLength);
        GridPane.setConstraints(crPathField, 1, 2);

        // Fourth row: Multi-line Text Area
        multiLineTextArea = new TextArea();
        multiLineTextArea.setPrefRowCount(8);
        multiLineTextArea.setWrapText(true);
        VBox multiLineVBox = new VBox(multiLineTextArea);
        GridPane.setConstraints(multiLineVBox, 0, 3, 3, 1);

        // Fifth row: Execute button, empty Label, QA Oentry button, another empty label, and Clear button
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> {
            orderByOentryModifyInfo();
            executeOentryUpdate();
        });
        Label emptyLabel1 = new Label();
        emptyLabel1.setPrefWidth(38);
        Button qaOentryButton = new Button("QA Oentry");
        Label emptyLabel2 = new Label();
        emptyLabel2.setPrefWidth(386);
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> logArea.clear());

        HBox buttonBox = new HBox(10, executeButton, emptyLabel1, qaOentryButton, emptyLabel2, clearButton);
        GridPane.setConstraints(buttonBox, 1, 4, 2, 1);

        // Sixth row: Log Area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox logVBox = new VBox(logArea);
        GridPane.setConstraints(logVBox, 0, 5, 3, 1);

        // Add all nodes to the grid
        grid.getChildren().addAll(
                filePathLabel, filePathField,
                crLabel, crField, guessButton,
                crPathLabel, crPathField,
                multiLineVBox,
                buttonBox,
                logVBox
        );

        // Set Tab order
        crField.setOnKeyPressed(event -> handleTabPress(event, crPathField));
        crPathField.setOnKeyPressed(event -> handleTabPress(event, multiLineTextArea));
        multiLineTextArea.setOnKeyPressed(event -> handleTabPress(event, executeButton));
        executeButton.setOnKeyPressed(event -> handleTabPress(event, qaOentryButton));
        qaOentryButton.setOnKeyPressed(event -> handleTabPress(event, clearButton));
        clearButton.setOnKeyPressed(event -> handleTabPress(event, crField));

        executorService = Executors.newFixedThreadPool(2);

        return new VBox(grid);
    }

    private void guessCrPath() {
        String crValue = crField.getText();
        if (crValue.isEmpty()) {
            showAlert("Error", "Please enter a CR number");
            return;
        }

        String currentYear = String.valueOf(Year.now().getValue());
        String updatedCrRootPath = crRootPath.replace("yyyy", currentYear);

        File crRootDir = new File(updatedCrRootPath);
        if (!crRootDir.exists() || !crRootDir.isDirectory()) {
            showAlert("Error", "CR root path does not exist: " + updatedCrRootPath);
            return;
        }

        File[] matchingDirs = crRootDir.listFiles((dir, name) -> name.startsWith(crValue));
        if (matchingDirs != null && matchingDirs.length > 0) {
            String crDir = matchingDirs[0].getAbsolutePath();
            crPathField.setText(crDir);
            crField.setText(getCrNumber(updatedCrRootPath, crDir));
        } else {
            crPathField.clear();
            showAlert("Error", "No matching directory found for CR: " + crValue, () -> {
                crField.requestFocus();
                crField.selectAll();
            });
        }
    }

    private String getCrNumber(String crRootPath, String crDir) {
        String remainingPart = crDir.substring(crRootPath.length() + 1);
        int endIndex = remainingPart.indexOf('-');
        if (endIndex != -1) {
            return remainingPart.substring(0, endIndex).trim();
        } else {
            return remainingPart.trim();
        }
    }

    private void orderByOentryModifyInfo() {
        List<String> lines = readLinesFromTextArea(multiLineTextArea);
        List<String> sortedLines = lines.stream()
                .filter(line -> parseRecord(line).length >= 4)
                .sorted(Comparator.comparing((String line) -> parseRecord(line)[0].trim())
                        .thenComparing(line -> parseRecord(line)[1].trim()))
                .collect(Collectors.toList());
        multiLineTextArea.setText(String.join("\n", sortedLines));
    }

    private void executeOentryUpdate() {
        executorService.submit(() -> {
            startTime = LocalDateTime.now();
            log("Execute start time: " + getCurrentTime(startTime));

            try {
                String crValue = crField.getText().trim();
                String crPath = crPathField.getText().trim();
                if (crValue.isEmpty() || crPath.isEmpty()) {
                    showAlert("Error", "CR value or CR Path is empty");
                    return;
                }

                String tkoTargetPath = crPath + "\\HKW20158319N01\\object\\app\\" + crValue + "\\cfg\\Oentry.cfg";
                String skmTargetPath = crPath + "\\HKW20158320N01\\object\\app\\" + crValue + "\\cfg\\Oentry.cfg";

                log("Copying tkoRealOentry to " + tkoTargetPath);
                copyFileWithAttributes(tkoRealOentry, tkoTargetPath);

                log("Copying skmRealOentry to " + skmTargetPath);
                copyFileWithAttributes(skmRealOentry, skmTargetPath);

                List<String> updates = readLinesFromTextArea(multiLineTextArea);
                if (updates.isEmpty()) {
                    log("No valid updates found in multiLineTextArea. Skipping config updates.");
                    return;
                }

                log("Updating TKO release Oentry.cfg");
                updateOentryConfig(tkoTargetPath, updates);

                log("Updating SKM release Oentry.cfg");
                updateOentryConfig(skmTargetPath, updates);

                log("Execute finished successfully.");
            } catch (Exception e) {
                log("Error during execute: " + e.getMessage());
                e.printStackTrace();
            }

            endTime = LocalDateTime.now();
            log("Execute end time: " + getCurrentTime(endTime));
            log("Time taken: " + getTimeTaken(startTime, endTime));
        });
    }

    private void copyFileWithAttributes(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        log("Copied file from " + sourcePath + " to " + targetPath);
    }

    private void updateOentryConfig(String configFilePath, List<String> updates) throws IOException {
        File configFile = new File(configFilePath);
        List<String> lines = new ArrayList<>();

        // Read the existing config file
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }

        // Parse updates and apply them to the config file content
        for (String update : updates) {
            if (update.trim().isEmpty()) continue;
            String[] parts = parseRecord(update);
            if (parts.length < 4) {
                log("Invalid update format: " + update);
                continue;
            }

            String section = parts[0].trim();
            String key = parts[1].trim();
            String value = parts[2].trim();
            String action = parts[3].trim().toLowerCase();

            boolean sectionFound = false;
            boolean keyFound = false;
            ListIterator<String> iterator = lines.listIterator();
            while (iterator.hasNext()) {
                String line = iterator.next();

                if (line.trim().equalsIgnoreCase("[" + section + "]")) {
                    sectionFound = true;

                    while (iterator.hasNext()) {
                        line = iterator.next();
                        if (line.trim().startsWith("[")) {
                            iterator.previous();
                            break;
                        }

                        String[] keyValue = line.split("=", 2);
                        if (keyValue.length == 2 && keyValue[0].trim().equals(key)) {
                            keyFound = true;

                            switch (action) {
                                case "update":
                                    iterator.set(key + "=" + value);
                                    log("Updated " + section + "." + key + " to " + value);
                                    break;
                                case "delete":
                                    iterator.remove();
                                    log("Deleted " + section + "." + key);
                                    break;
                            }
                            break;
                        }
                    }

                    if (!keyFound && (action.equals("insert") || action.equals("add"))) {
                        if (iterator.hasPrevious() && !iterator.previous().trim().isEmpty()) {
                            iterator.next();
                        }
                        iterator.add(key + "=" + value);
                        log("Inserted " + section + "." + key + " with " + value);
                    }
                    break;
                }
            }

            if (!sectionFound && (action.equals("insert") || action.equals("add"))) {
                lines.add("[" + section + "]");
                lines.add(key + "=" + value);
                log("Added section " + section + " with " + key + "=" + value);
            }
        }

        // Write the updated config file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private String[] parseRecord(String record) {
        String[] fields = new String[4];
        int start = 0, end = 0, fieldIndex = 0;

        while (fieldIndex < 4) {
            if (record.charAt(start) == '\"') {
                end = start + 1;
                while (end < record.length() && record.charAt(end) != '\"') {
                    end++;
                }
                fields[fieldIndex] = record.substring(start + 1, end);
                end += 2; // Skip over the closing quote and the comma
            } else {
                end = start;
                while (end < record.length() && record.charAt(end) != ',') {
                    end++;
                }
                fields[fieldIndex] = record.substring(start, end);
                end++; // Skip over the comma
            }
            start = end;
            fieldIndex++;
        }
        return fields;
    }

    private List<String> readLinesFromTextArea(TextArea textArea) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(textArea.getText()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            log("Error reading lines from TextArea: " + e.getMessage());
        }
        return lines;
    }

    public void stop() {
        executorService.shutdown();
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

    private void showAlert(String title, String message) {
        showAlert(title, message, null);
    }

    private void showAlert(String title, String message, Runnable onClose) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait().ifPresent(response -> {
                if (onClose != null) {
                    onClose.run();
                }
            });
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
