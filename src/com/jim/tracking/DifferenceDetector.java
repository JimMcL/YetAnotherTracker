package com.jim.tracking;

import com.jim.Params;
import com.jim.ui.OptionalFeedbackWindow;
import com.jim.util.Util;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Detects objects which differ from a background frame.
 */
public class DifferenceDetector implements MotionDetector.Detector {
    private static final int MAX_CONTOURS = 1000;
    private OptionalFeedbackWindow fb = new OptionalFeedbackWindow("Hierarchy", true);
    //    private Mat foregroundFrame;

    /** Returns centroids of detected contours.
     * Various feedback graphics are optionally (based on <code>opts</code>) drawn to <code>feedbackImage</code>.
     * @return List of points of centroids of contours which have area greater than <code>minArea</code>.
     * */
    @Override
    public ArrayList<MotionDetector.DetectedObject> detect(int frameIndex, Mat greyFrame, Mat colourFrame, Params params, Mat feedbackImage) {

        Mat foregroundFrame = params.trParams.foregroundSegmenter.segment(frameIndex, greyFrame, colourFrame, params);

        // Apply expansion/contraction to foreground areas
        for (double v : params.trParams.dilationErosionSize) {
            if (v > 0) {
                // Expand regions
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(v, v));
                Imgproc.dilate(foregroundFrame, foregroundFrame, kernel, new Point(-1, -1), 1);
            } else if (v < 0) {
                // Contract regions
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(-v, -v));
                Imgproc.erode(foregroundFrame, foregroundFrame, kernel, new Point(-1, -1), 1);
            }
        }

        params.grParams.thresholdFeedback.maybeShowFrame(foregroundFrame);

        return detect(foregroundFrame, params, feedbackImage);
    }

    private ArrayList<MotionDetector.DetectedObject> detect(Mat image, Params params, Mat feedbackImage) {

        // Construct contours around thresholded differences
        Mat hierarchy = new Mat();
        Mat imageCopy = image.clone();
        List<MatOfPoint> contours = new ArrayList<>();
        final int mode = Imgproc.RETR_EXTERNAL;
        final int method = Imgproc.CHAIN_APPROX_SIMPLE;
        Imgproc.findContours(imageCopy, contours, hierarchy, mode, method);
//        fb.maybeShowFrame(hierarchy);
        hierarchy.release();
        imageCopy.release();

        ArrayList<MotionDetector.DetectedObject> result = new ArrayList<>();

        // For each contour...
        final int numContours = contours.size();
        for (int idx = 0; idx < numContours; idx++) {
            MatOfPoint contour = contours.get(idx);
            double contourArea = Imgproc.contourArea(contour);
            // Ignore contours which are too small or large
            boolean tooSmall = contourArea < params.trParams.minContourArea;
            boolean tooBig = contourArea > params.trParams.maxContourArea;
            boolean tooShort = false;
            boolean tooLong = false;
            MatOfPoint2f contour2f = null;
            double contourLength = 0;
            if (!tooSmall && !tooBig) {
                contour2f = new MatOfPoint2f();
                contour2f.fromList(contour.toList());
                contourLength = Imgproc.arcLength(contour2f, true);
                tooShort = contourLength < params.trParams.minContourLength;
                tooLong = contourLength > params.trParams.maxContourLength;
            }
            if (!tooSmall && !tooBig && !tooShort && !tooLong) {
                // Don't even try to handle lots of detections
                if (result.size() > MAX_CONTOURS) {
                    if (params.grParams.debug)
                        System.out.println("Skipping contours after first " + MAX_CONTOURS);
                    break;
                }

                // Calculate contour centroid
                Moments mu = Imgproc.moments(contour);
                Point centroid = new Point(mu.get_m10() / mu.get_m00(), mu.get_m01() / mu.get_m00());
                // Create a new instance with the centroid
                final MotionDetector.DetectedObject trackedObject = new MotionDetector.DetectedObject(centroid);

                // Get ellipse (if there are enough points detected)
                if (contour.total() >= 5) {
                    final RotatedRect ell = Imgproc.fitEllipse(contour2f);
                    // Check if ellipse is too big
                    if (ell.size.width > params.trParams.maxLength || ell.size.height > params.trParams.maxLength)
                        continue;
                    trackedObject.ellipse = ell;
                    if (params.grParams.showEllipse)
                        Imgproc.ellipse(feedbackImage, trackedObject.ellipse, Util.RGB(0, 255, 0));
                }

                result.add(trackedObject);

                double epsilon = .01 * contourLength;
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approx, epsilon, true);
                Imgproc.drawContours(feedbackImage, Arrays.asList(Util.matOfPoint2fToMatOfPoint(approx)), 0, Util.RGB(0, 255, 80));

                // Feedback
                if (params.grParams.showContours)
                    Imgproc.drawContours(feedbackImage, contours, idx, Util.RGB(255, 0, 0));
                if (params.grParams.showRectangle) {
                    Rect br = Imgproc.boundingRect(contour);
                    Imgproc.rectangle(feedbackImage, br.br(), br.tl(), Util.RGB(0, 255, 0), 1);
                }
                if (params.grParams.showCentroid)
                    Imgproc.circle(feedbackImage, centroid, 5, Util.RGB(0, 0, 255));

            } else if (params.grParams.debug) {
                if (contourArea > 0) {
                    String exc = tooBig ? "big" :
                            tooSmall ? "small" :
                            tooShort ? "short" :
                            "long";
                    System.out.println("Skipping contour " + String.format("%03d", idx) + ", too " + exc + ", size " + contourArea + ", length " + contourLength);
                }
            }
        }

        return result;
    }
}
