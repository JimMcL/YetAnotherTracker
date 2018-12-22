package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static com.jim.util.Util.RGB;
import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;

/**
 * Created by jim_m on 05-Jun-17.
 */
public class DebugOverlay implements MotionDetector.Filter {
    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {
        int curr = (int) (100 * camera.getFrameIndex() / camera.getNumOfFrames());
        Imgproc.putText(feedbackImage, (curr + "%"), new Point(10, 120), FONT_HERSHEY_SIMPLEX, .6, RGB(200, 0, 0));
    }

    @Override
    public void onCameraOpened(VideoPlayer camera) {
    }

    @Override
    public void onDone(VideoPlayer camera) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " - displays progress on video window";
    }
}
