package com.infott.filebot.sample;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class TaskbarIconExample extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Taskbar Icon Example");

        // Attempt to set taskbar icon and print debug information
        try {
            URL iconUrl = getClass().getResource("/icon.bmp");
            if (iconUrl == null) {
                showError("Icon loading failed", "The icon URL could not be found.");
                return;
            }

            Image icon = new Image(iconUrl.toExternalForm());
            if (icon.isError()) {
                showError("Icon loading failed", "The icon image could not be loaded. URL: " + iconUrl);
                System.out.println("Error details: " + icon.getException().getMessage());
            } else {
                primaryStage.getIcons().add(icon);
                System.out.println("Icon loaded successfully from URL: " + iconUrl);
            }
        } catch (Exception e) {
            showError("Exception occurred", e.getMessage());
            e.printStackTrace();
        }

        Label label = new Label("Hello, JavaFX!");
        Scene scene = new Scene(label, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showError(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}


