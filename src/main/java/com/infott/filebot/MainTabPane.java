package com.infott.filebot;

import com.infott.filebot.csv.CsvHandler;
import com.infott.filebot.filecopier.FileCopier;
import com.infott.filebot.oentry.OentryReleaser;
import com.infott.filebot.sql.SQLInserter;
import com.infott.filebot.unzip.UnzipManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@SuppressWarnings("restriction")
public class MainTabPane extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Bot");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.bmp")));

        TabPane tabPane = new TabPane();

        // First tab with FileCopier
        Tab fileCopierTab = new Tab();
        fileCopierTab.setText("File Copier");

        FileCopier fileCopier = new FileCopier();
        VBox fileCopierVBox = fileCopier.createFileCopierUI();
        
        fileCopierTab.setContent(fileCopierVBox);
        fileCopierTab.setClosable(false);

        // Second tab with UnzipManager
        Tab unzipTab = new Tab();
        unzipTab.setText("Unzip Manager");

        UnzipManager unzipManager = new UnzipManager();
        VBox unzipVBox = unzipManager.createUnzipUI();
        
        unzipTab.setContent(unzipVBox);
        unzipTab.setClosable(false);

        // Third tab with OentryReleaser
        Tab oentryReleaserTab = new Tab();
        oentryReleaserTab.setText("Oentry Releaser");

        OentryReleaser oentryReleaser = new OentryReleaser();
        VBox oentryReleaserVBox = oentryReleaser.createOentryReleaserUI();
        
        oentryReleaserTab.setContent(oentryReleaserVBox);
        oentryReleaserTab.setClosable(false);
        
        // Fourth tab with CsvHandler
        Tab csvHandlerTab = new Tab();
        csvHandlerTab.setText("CSV Handler");

        CsvHandler csvHandler = new CsvHandler();
        VBox csvHandlerVBox = csvHandler.createCsvHandlerUI();
        
        csvHandlerTab.setContent(csvHandlerVBox);
        csvHandlerTab.setClosable(false);
        
        // 5th tab with SQLInserter
        Tab sqlInserterTab = new Tab();
        sqlInserterTab.setText("SQL Inserter");

        SQLInserter sqlInserter = new SQLInserter();
        VBox sqlInserterVBox = sqlInserter.createSQLInserterUI();
        
        sqlInserterTab.setContent(sqlInserterVBox);
        sqlInserterTab.setClosable(false);

        tabPane.getTabs().add(fileCopierTab);
        tabPane.getTabs().add(unzipTab);
        tabPane.getTabs().add(oentryReleaserTab);
        tabPane.getTabs().add(csvHandlerTab);
        tabPane.getTabs().add(sqlInserterTab);

        Scene scene = new Scene(tabPane, 800, 450);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        FileCopier fileCopier = new FileCopier();
        fileCopier.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}