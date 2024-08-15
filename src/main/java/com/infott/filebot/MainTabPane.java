package com.infott.filebot;

import com.infott.filebot.csv.CsvHandler;
import com.infott.filebot.filecopier.FileCopier;
import com.infott.filebot.hc.HTTPHealthChecker;
import com.infott.filebot.oentry.OentryReleaser;
import com.infott.filebot.sql.CsvGenInsertSQL;
import com.infott.filebot.sql.DescToSQL;
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

        TabPane mainTabPane = new TabPane();

        Tab group1Tab = genGroup1Tab();
        Tab group2Tab = genGroup2Tab();
        Tab group3Tab = genGroup3Tab();

        mainTabPane.getTabs().addAll(group1Tab, group2Tab, group3Tab);

        Scene scene = new Scene(mainTabPane, 800, 450);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

	private Tab genGroup3Tab() {
		// Group 3 TabPane
        TabPane group3TabPane = new TabPane();
        
        Tab httpHCTab = new Tab();
        httpHCTab.setText("HTTP HC");
        
        HTTPHealthChecker httpHC = new HTTPHealthChecker();
        VBox httpHCVBox = httpHC.createHTTPHealthCheckUI();
        
        httpHCTab.setContent(httpHCVBox);
        httpHCTab.setClosable(false);
        
        group3TabPane.getTabs().addAll(httpHCTab);
        
        Tab group3Tab = new Tab("Group 3");
        group3Tab.setContent(group3TabPane);
        group3Tab.setClosable(false);
		return group3Tab;
	}

	private Tab genGroup2Tab() {
		// Group 2 TabPane
        TabPane group2TabPane = new TabPane();

        // 4th tab with DescToSQL
        Tab descToSQLTab = new Tab();
        descToSQLTab.setText("Desc To SQL");

        DescToSQL descToSQL = new DescToSQL();
        VBox descToSQLVBox = descToSQL.createDescToSQLUI();

        descToSQLTab.setContent(descToSQLVBox);
        descToSQLTab.setClosable(false);

        // 5th tab with CsvHandler
        Tab csvHandlerTab = new Tab();
        csvHandlerTab.setText("CSV Handler");

        CsvHandler csvHandler = new CsvHandler();
        VBox csvHandlerVBox = csvHandler.createCsvHandlerUI();

        csvHandlerTab.setContent(csvHandlerVBox);
        csvHandlerTab.setClosable(false);

        // 6th tab with CsvGenInsertSQL
        Tab csvGenInsertSQLTab = new Tab();
        csvGenInsertSQLTab.setText("CSV Gen Insert");

        CsvGenInsertSQL csvGenInsertSQL = new CsvGenInsertSQL();
        VBox csvGenInsertSQLVBox = csvGenInsertSQL.createCsvToInsertSQLUI();

        csvGenInsertSQLTab.setContent(csvGenInsertSQLVBox);
        csvGenInsertSQLTab.setClosable(false);

        // 7th tab with SQLInserter
        Tab sqlInserterTab = new Tab();
        sqlInserterTab.setText("SQL Inserter");

        SQLInserter sqlInserter = new SQLInserter();
        VBox sqlInserterVBox = sqlInserter.createSQLInserterUI();

        sqlInserterTab.setContent(sqlInserterVBox);
        sqlInserterTab.setClosable(false);

        group2TabPane.getTabs().addAll(descToSQLTab, csvHandlerTab, csvGenInsertSQLTab, sqlInserterTab);

        Tab group2Tab = new Tab("Group 2");
        group2Tab.setContent(group2TabPane);
        group2Tab.setClosable(false);
		return group2Tab;
	}

	private Tab genGroup1Tab() {
		// Group 1 TabPane
        TabPane group1TabPane = new TabPane();
        
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

        group1TabPane.getTabs().addAll(fileCopierTab, unzipTab, oentryReleaserTab);

        Tab group1Tab = new Tab("Group 1");
        group1Tab.setContent(group1TabPane);
        group1Tab.setClosable(false);
		return group1Tab;
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
