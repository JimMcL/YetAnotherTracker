package com.jim.ui;

import com.jim.Params;
import com.jim.tracking.MotionDetector;
import com.jim.tracking.TrackWithEllipse;
import com.jim.tracking.VideoPlayer;
import javafx.application.Platform;
import org.opencv.core.Mat;

import java.util.List;

public class HeadlessController implements MotionDetector.Filter {
    // Video + all tracking and debugging parameters
    protected Params params;

    @Override
    public void onCameraOpened(VideoPlayer camera) {
    }

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {
    }

    @Override
    public void onDone(VideoPlayer camera) {
        Platform.exit();
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public Params getParams() {
        return params;
    }
}
