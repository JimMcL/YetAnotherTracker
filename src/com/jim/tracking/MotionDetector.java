package com.jim.tracking;

import com.jim.Params;
import com.jim.util.ObserverMgr;
import com.jim.util.Util;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* Loosely based on:
    https://ratiler.wordpress.com/2014/09/08/detection-de-mouvement-avec-javacv/
    http://www.pyimagesearch.com/2015/05/25/basic-motion-detection-and-tracking-with-python-and-opencv/
*/
@SuppressWarnings("WeakerAccess")
public class MotionDetector implements FrameLoop.Handler {
    public FeatureDetector fDetector;
    public VideoPlayer cameraInfo;
    private Params params;

    private final int KEYPOINT_RADIUS_DENOM = 2;

    public interface Filter {
        void onCameraOpened(VideoPlayer camera);
        void handle(List<DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera);
        void onDone(VideoPlayer camera);
    }

    public static class DetectedObject {
        RotatedRect ellipse;
        Point centroid;
        public KeyPoint keyPoint;   // Closest key point to the moving object
        public double keyPointDistance; // Distance from centroid to keyPoint

        public DetectedObject(Point centroid) {
            this.centroid = centroid;
        }

        public Point currentPos() {
            return centroid;
//            return keyPoint.pt;
//            return new Point((centroid.x + keyPoint.pt.x) / 2, (centroid.y + keyPoint.pt.y) / 2);
        }
    }

    public interface Detector {
        ArrayList<DetectedObject> detect(int frameIndex, Mat greyFrame, Mat colourFrame, Params params, Mat feedbackImage);
    }

    public void run(Params params) throws IOException {
        this.params = params;
        new FrameLoop().run(this, params.srcParams.videoFile, params.grParams, params.srcParams, params.trParams);
//        if (params.grParams.verbose)
//            System.out.println("Finished");
    }

    // ==========================================================================
    // FrameLoop.Handler methods

    @Override
    public void onVideoOpened(VideoPlayer videoPlayer) {
        cameraInfo = videoPlayer;

        fDetector = new FeatureDetector();

        for (Filter filter : params.trParams.filters)
            filter.onCameraOpened(cameraInfo);

        if (params.grParams.verbose)
            System.out.println("Input " + params.srcParams.videoFile + ", fps = " + cameraInfo.getFps() + ", resolution " + cameraInfo.getfWidth() + "x" + cameraInfo.getfHeight());

        updateTransformedMask();
        ObserverMgr.getInstance().observe(params.trParams, object -> updateTransformedMask());
    }

    @Override
    public void onFrame(Mat greyFrame, Mat colourFrame) {
        if (params.grParams.verbose && params.grParams.running)
            logProgress(cameraInfo.getFrameIndex());

        if (cameraInfo.getFrameIndex() % 100 == 0)
            // Essential. When no graphics, memory use just increases until the computer locks up!
            System.gc();

        Mat feedbackImage = params.grParams.feedbackGrey ? greyFrame.clone() : colourFrame.clone();

        // Apply mask.
        // Ideally, this would be done on the raw frame (i.e. in com.jim.tracking.FrameLoop.run,
        // before rotation, scaling, blur), however that messes up the feedback image
        synchronized (params.trParams) {
            Region tm = params.trParams.getTransformedMask();
            if (tm != null) {
                greyFrame = tm.mask(greyFrame, params.trParams.thresholdInvert ? Util.WHITE : Util.BLACK);
                colourFrame = tm.mask(colourFrame, params.trParams.thresholdInvert ? Util.WHITE : Util.BLACK);
                tm.draw(feedbackImage, Util.RGB(200, 0, 0), null);
            }
        }

        // Find moving objects
        List<DetectedObject> objects = params.trParams.detector.detect(cameraInfo.getFrameIndex(), greyFrame, colourFrame, params, feedbackImage);

        // This is purely experimental and can't be turned on by the user
        if (params.trParams.correlateObjectsWithFeatures) {
            // Find features in grey frame
            KeyPoint[] keyPoints = fDetector.detect(greyFrame, feedbackImage, params.grParams.showFeatures ? (1.0 / KEYPOINT_RADIUS_DENOM) : 0);
            // Match up moving objects with features - motion without a feature is probably just noise
            objects = matchObjectsWithFeatures(objects, keyPoints);
        }

        // Apply any filters in order
        List<TrackWithEllipse> trackedObjects = new ArrayList<>();
        for (Filter filter : params.trParams.filters) {
            filter.handle(objects, trackedObjects, greyFrame, feedbackImage, params, cameraInfo);
        }

        // Pass the frame to the background handler
        params.trParams.backgroundHandler.processFrame(
                params.trParams.backgroundHandler.canHandleColour() ? colourFrame : greyFrame);

        feedbackImage.release();
    }

    @Override
    public void onDone() {
        // Apply any filters in order
        for (Filter filter : params.trParams.filters)
            filter.onDone(cameraInfo);
    }

    // ==========================================================================
    // Private methods

    private void logProgress(int frameIndex) {
        // Log changes every 10% of progress
        int prev = (int) (10 * (frameIndex - 1) / cameraInfo.getNumOfFrames());
        int curr = (int) (10 * frameIndex / cameraInfo.getNumOfFrames());
        if (prev != curr)
            System.out.println(curr * 10 + "%");
    }

    /** Filters out detected moving objects which aren't associated with a feature keypoint.
     *
     * @param objects Detected moving objects
     * @param keyPoints Static feature keypoints
     * @return list of meaningful detected objects.
     */
    private List<DetectedObject> matchObjectsWithFeatures(List<DetectedObject> objects, KeyPoint[] keyPoints) {
        List<MotionDetector.DetectedObject> result = new ArrayList<>();

        // For each detected object...
        for (MotionDetector.DetectedObject object : objects) {
            Point objPos = object.centroid;
            // For each keypoint...
            for (KeyPoint keyPoint : keyPoints) {
                // Calculate distance from object to keypoint
                double dist = Util.distance(objPos, keyPoint.pt);
                // If object is within significant keypoint radius, use it
                if (dist < keyPoint.size / KEYPOINT_RADIUS_DENOM) {
                    if (object.keyPoint == null)
                        result.add(object);
                    // Is it the closest keypoint?
                    if (object.keyPoint == null || object.keyPointDistance > dist) {
                        object.keyPoint = keyPoint;
                        object.keyPointDistance = dist;
                    }
                    break;
                }
            }
        }
        return result;
    }

    private void updateTransformedMask() {
        Params.TrackerParams prm = params.trParams;
        synchronized (prm) {
            prm.setTransformedMask(null);
            if (prm.getMask() != null)
                prm.setTransformedMask(prm.getMask().transformForParams(params.srcParams, new Size(cameraInfo.getfWidth(), cameraInfo.getfHeight()), false));
        }
    }
}