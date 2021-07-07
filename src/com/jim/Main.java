package com.jim;

import com.jim.ui.HeadlessController;
import com.jim.ui.MainController;
import com.jim.ui.TrackerService;
import com.jim.util.WindowPos;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;

import static com.jim.util.Util.basename;

public class Main extends Application {


    private static Params params;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/fxml/Tracker.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Yet Another Video Tracker - " + basename(params.srcParams.videoFile));
        primaryStage.setScene(new Scene(root));

        // Setup a controller - either headless or a window
        HeadlessController controller;
        if (params.grParams.showWindow) {
            primaryStage.show();
            WindowPos.rememberWindowOrigin(primaryStage, MainController.class, null);

            MainController mainController = loader.getController();
            mainController.setStage(primaryStage);
            controller = mainController;
        } else {
            controller = new HeadlessController();
        }
        params.trParams.filters.add(controller);
        controller.setParams(params);

        // Create a worker which does all the tracking work, and drives updates to the GUI
        TrackerService tracker = new TrackerService();
        tracker.setParams(params);
        tracker.start();
    }

    public static void main(String[] args) throws IOException {

        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if (Core.getVersionMajor() < 3 || (Core.getVersionMajor() == 3 && Core.getVersionMinor() < 4))
            throw new RuntimeException("Require OpenCV version 3.4 or later, " +
                    Core.getVersionString() + " is installed");

        params = ParamsBuilder.build(args);

        launch(args);
    }
}
