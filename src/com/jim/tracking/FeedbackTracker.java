package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static com.jim.util.Util.RGB;
import static org.opencv.core.Core.BORDER_CONSTANT;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.warpAffine;

/**
 * Centres the feedback view on the tracked object.
 */
public class FeedbackTracker implements MotionDetector.Filter {
    private Mat trackingTransform = null;

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params params, VideoPlayer camera) {
        Size dsize = params.grParams.playbackSize;

        // Pick the longest track so far
        double longest = 0;
        TrackWithEllipse longestTrack = null;
//        System.out.println("Tracks: " + Util.join(trackedObjects, ", "));
        for (TrackWithEllipse trackedObject : tracks) {
            final double displacement = trackedObject.getDisplacement();
            if (displacement > longest) {
                longest = displacement;
                longestTrack = trackedObject;
            }
        }
        // Calculate new tracking transform
        if (longestTrack != null) {
            // Translate so that centroid is in the centre of the frame
            RotatedRect ellipse = longestTrack.getEllipse();
            Point p = ellipse == null ? longestTrack.getCurrentPosition() : ellipse.center;

            // Map centre of ellipse to centre of frame
            MatOfPoint2f srcPoints;
            if (params.grParams.rotateToTrack)
                srcPoints = new MatOfPoint2f(ellipse.center,
                        translatePoint(ellipse.center, ellipse.angle, 1),
                        translatePoint(ellipse.center, ellipse.angle - 90, 1));
            else
                srcPoints = new MatOfPoint2f(p,
                        new Point(p.x, p.y + 1),
                        new Point(p.x + 1, p.y));
            Point frameCentre = new Point(dsize.width / 2, dsize.height / 2);
            MatOfPoint2f dstPoints = new MatOfPoint2f(frameCentre,
                    new Point(frameCentre.x, frameCentre.y + 1),
                    new Point(frameCentre.x + 1, frameCentre.y));
            trackingTransform = Imgproc.getAffineTransform(srcPoints, dstPoints);
        }

        if (trackingTransform != null) {
            // Apply the tracking transform, make areas outside the original image white
            warpAffine(feedbackImage, feedbackImage, trackingTransform, dsize, INTER_LINEAR, BORDER_CONSTANT, RGB(255, 255, 255));
        }
    }

    @Override
    public void onCameraOpened(VideoPlayer camera) {
    }

    @Override
    public void onDone(VideoPlayer camera) {
    }

    @Override
    public String toString() {
        return "Feedback tracker - centres output frame on object";
    }

    private Point translatePoint(Point p, double angle, double dist) {
        angle = angle * Math.PI / 180;
        return new Point(p.x + dist * Math.cos(angle), p.y + dist * Math.sin(angle));
    }
}
