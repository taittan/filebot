package com.infott.filebot.hc;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("restriction")
public class HTTPHealthChecker {

    private static final int DEFAULT_FREQUENCY = 60;
    private List<APIChecker> apiCheckers;
    private TextField frequencyField;
    private TextArea logArea;
    private Timer timer;
    private ToggleGroup toggleGroup;

    public VBox createHTTPHealthCheckUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        apiCheckers = new ArrayList<>();
        toggleGroup = new ToggleGroup();
        int apiCount = 16;

        for (int i = 0; i < apiCount; i++) {
            String apiUrl = "http://www.baidu.com"; // Replace with actual URLs
            if(i == 3) {
            	apiUrl = "http://www.xjshu283736.com";
            }
            APIChecker checker = new APIChecker("API " + (i + 1), apiUrl);
            apiCheckers.add(checker);

            HBox apiBox = new HBox(5, checker.getRadioButton(), checker.getLabel(), checker.getIndicator());
            grid.add(apiBox, i % 8, (i / 8));
            checker.getRadioButton().setToggleGroup(toggleGroup);
        }

        // Frequency input and execute button
        Label frequencyLabel = new Label("Execution Frequency (seconds):");
        frequencyField = new TextField(String.valueOf(DEFAULT_FREQUENCY));
        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> executeAPI());

        HBox frequencyBox = new HBox(10, frequencyLabel, frequencyField, executeButton);
        GridPane.setConstraints(frequencyBox, 0, (apiCount / 8) + 1, 8, 1);

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        GridPane.setConstraints(logArea, 0, (apiCount / 8) + 2, 8, 1);

        // Add all elements to the grid
        grid.getChildren().addAll(frequencyBox, logArea);

        VBox container = new VBox(grid);
        container.setPadding(new Insets(10));
        container.setSpacing(10);

        // Schedule API checks
        timer = new Timer(true);
        scheduleAPIChecks(DEFAULT_FREQUENCY);

        return container;
    }

    private void executeAPI() {
        int frequency;
        try {
            frequency = Integer.parseInt(frequencyField.getText().trim());
            if (frequency <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            log("Invalid frequency value. Please enter a positive integer.");
            return;
        }

        // Reschedule API checks with the new frequency
        timer.cancel();
        timer = new Timer(true);
        scheduleAPIChecks(frequency);

        // Check selected APIs immediately
        for (APIChecker checker : apiCheckers) {
            if (checker.getRadioButton().isSelected()) {
                checker.checkAPI();
            }
        }
    }

    private void scheduleAPIChecks(int frequency) {
        for (APIChecker checker : apiCheckers) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000); // Delay for 10 seconds
                        Platform.runLater(checker::checkAPI);
                    } catch (InterruptedException e) {
                        log("Error during delay: " + e.getMessage());
                    }
                }
            }, 0, frequency * 1000L);
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private class APIChecker {
        private final RadioButton radioButton;
        private final Label label;
        private final Circle indicator;
        private final String apiName;
        private final String apiUrl;

        public APIChecker(String apiName, String apiUrl) {
            this.apiName = apiName;
            this.apiUrl = apiUrl;
            this.radioButton = new RadioButton();
            this.label = new Label(apiName);
            this.indicator = new Circle(10, Color.RED);
        }

        public RadioButton getRadioButton() {
            return radioButton;
        }

        public Label getLabel() {
            return label;
        }

        public Circle getIndicator() {
            return indicator;
        }

        public void checkAPI() {
            boolean isHealthy = callAPI();

            // Update UI
            Platform.runLater(() -> {
                indicator.setFill(isHealthy ? Color.GREEN : Color.RED);
                if (radioButton.isSelected()) {
                    log(apiName + " response: " + (isHealthy ? "Healthy" : "Unhealthy"));
                }
            });
        }

        private boolean callAPI() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                connection.disconnect();
                return responseCode == 200;
            } catch (IOException e) {
                log("Error calling API " + apiName + ": " + e.getMessage());
                return false;
            }
        }
    }
}
