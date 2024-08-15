package com.infott.filebot.sample;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TabManager extends Application {

    @Override
    public void start(Stage primaryStage) {
        TabPane mainTabPane = new TabPane();

        // Group 1 TabPane
        TabPane group1TabPane = new TabPane();
        group1TabPane.getTabs().addAll(
                createTab("Tab 1.1", "Content for Tab 1.1"),
                createTab("Tab 1.2", "Content for Tab 1.2"),
                createTab("Tab 1.3", "Content for Tab 1.3")
        );
        Tab group1Tab = new Tab("Group 1", group1TabPane);

        // Group 2 TabPane
        TabPane group2TabPane = new TabPane();
        group2TabPane.getTabs().addAll(
                createTab("Tab 2.1", "Content for Tab 2.1"),
                createTab("Tab 2.2", "Content for Tab 2.2")
        );
        Tab group2Tab = new Tab("Group 2", group2TabPane);

        // Main TabPane
        mainTabPane.getTabs().addAll(group1Tab, group2Tab);

        BorderPane root = new BorderPane();
        root.setCenter(mainTabPane);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Tab Manager Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createTab(String title, String content) {
        Tab tab = new Tab(title);
        Label label = new Label(content);
        tab.setContent(label);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
