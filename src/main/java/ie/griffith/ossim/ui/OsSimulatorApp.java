package ie.griffith.ossim.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/** Entry point. Each tab is a separate simulator; all logic lives outside the ui package. */
public class OsSimulatorApp extends Application {
    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane(
                new Tab("CPU Scheduling", new SchedulingPane()),
                new Tab("Page Replacement", new PagingPane()),
                new Tab("Banker's Algorithm", new BankersPane()),
                new Tab("File System", new FileSystemPane()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        stage.setTitle("Mini OS Simulator");
        stage.setScene(new Scene(tabs, 1000, 650));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
