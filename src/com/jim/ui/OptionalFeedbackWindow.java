package com.jim.ui;

import javafx.application.Platform;
import org.opencv.core.Mat;

import java.io.IOException;

import static com.jim.ui.FeedbackWindow.createWindow;

/** Basic wrapper around a FeedbackWindow which allows the user to specify whether or not a particular feedback window should be displayed. */
public class OptionalFeedbackWindow {
    private String title;
    private boolean show;
    private FeedbackWindow window;

    public OptionalFeedbackWindow(String title, boolean show) {
        this.title = title;
        this.show = show;
    }

    public void maybeShowFrame(Mat frame) {
        if (show && frame != null) {
            if (window == null) {
                Platform.runLater(() -> {
                    try {
                        window = createWindow(title, frame.size());
                        window.displayFrame(frame);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                window.displayFrame(frame);
            }
        }
    }
}
