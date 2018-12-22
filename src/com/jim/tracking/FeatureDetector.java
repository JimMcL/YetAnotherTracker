package com.jim.tracking;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import static com.jim.util.Util.RGB;
import static org.opencv.imgproc.Imgproc.goodFeaturesToTrack;

/**
 * Created by jim_m on 09-Jun-17.
 */
public class FeatureDetector {
    // If true, use Shi-Tomasi feature detection, otherwise use ORB
    private static final boolean USE_GOOD_FEATURES = true;
    private ORB detector;
//    private final DescriptorExtractor extractor;

    public FeatureDetector() {
        detector = ORB.create();
//        extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    }

    public KeyPoint[] detect(Mat img, Mat feedbackImage, double dbgRadiusFactor) {
        final KeyPoint[] result;
        if (USE_GOOD_FEATURES) {
            // Shi-Tomasi feature detection
            MatOfPoint corners = new MatOfPoint();
            // Assume feature of interest is one of the best in the image
            goodFeaturesToTrack(img, corners, 10, 0.3, 7);
            final Point[] points = corners.toArray();
            result = new KeyPoint[points.length];
            for (int i = 0; i < points.length; i++) {
                Point point = points[i];
                result[i] = new KeyPoint((float)point.x, (float)point.y, 30);
            }

            if (dbgRadiusFactor > 0)
                for (Point point : points) {
                    Imgproc.circle(feedbackImage, point, 10, RGB(255, 100, 200));
                }
        } else {
            MatOfKeyPoint keyPoints = new MatOfKeyPoint();
            detector.detect(img, keyPoints);
            /* Broken in OpenCV 4.0.0
            if (dbgRadiusFactor > 0)
                Features2d.drawKeypoints(img, keyPoints, feedbackImage, RGB(255, 200, 100), DRAW_RICH_KEYPOINTS);
//            for (KeyPoint k : keyPoints.toArray()) {
//                Imgproc.drawMarker(feedbackImage, k.pt, RGB(255, 0, 120), MARKER_TILTED_CROSS, 20, 1, 8);
//                Imgproc.circle(feedbackImage, k.pt, (int) (k.size * dbgRadiusFactor), RGB(255, 0, 255));
//            }
*/

//        Mat descriptors = new Mat();
//        extractor.compute(img, keyPoints, descriptors);
            result = keyPoints.toArray();
        }
        return result;
    }
}
