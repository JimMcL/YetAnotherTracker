package com.jim.ui;

import com.jim.util.Util;
import com.jim.util.WindowPos;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.IOException;

/** Simple window which knows how to display an OpenCV Mat as an image. */
public class FeedbackWindow {

    @FXML
    private ImageView imageView;
    @FXML
    private ScrollPane scrollPane;

    static FeedbackWindow createWindow(String title, Size size) throws IOException {

        double ar = size.height / size.width;
        // Ensure stage fits on the screen
        double width = Util.constrainedWidthToScreen(900, ar);

        FXMLLoader loader = new FXMLLoader(FeedbackWindow.class.getResource("fxml/FeedbackWindow.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, ar * width));
        stage.show();
        WindowPos.rememberWindowOrigin(stage, FeedbackWindow.class, title);

        return loader.getController();
    }

    public void initialize() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        imageView.fitHeightProperty().bind(scrollPane.heightProperty());
        imageView.fitWidthProperty().bind(scrollPane.widthProperty());
    }

    /**
     * Displays a single image.
     */
    void displayFrame(Mat frame) {
        if (frame != null) {
            Image image = Util.mat2BufferedImage(frame);
            Platform.runLater(() -> imageView.imageProperty().set(image));
        }
    }

    // =======================================================================
    // Private methods
}
