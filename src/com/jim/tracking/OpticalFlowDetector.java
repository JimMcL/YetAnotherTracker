package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static com.jim.util.Util.idToRGB;
import static org.opencv.core.CvType.CV_32FC2;
import static org.opencv.imgproc.Imgproc.goodFeaturesToTrack;
import static org.opencv.video.Video.calcOpticalFlowPyrLK;

/**
 * Created by jim_m on 10-Jun-17.
 */
public class OpticalFlowDetector implements MotionDetector.Detector {
    private MatOfPoint2f oldFeatures;
    private Mat oldFrame;
    private Mat feedbackMask;

    @Override
    public ArrayList<MotionDetector.DetectedObject> detect(int frameIndex, Mat greyFrame, Mat colourFrame, Params params, Mat feedbackImage) {
        ArrayList<MotionDetector.DetectedObject> result = new ArrayList<>();

        // Ensure we have the features being tracked
        boolean needFeatures = oldFeatures == null || frameIndex % 5 == 0;
        if (needFeatures) {
            // Shi-Tomasi feature detection
            // Assume feature of interest is one of the best in the image
            MatOfPoint features = new MatOfPoint();
            goodFeaturesToTrack(oldFrame == null ? greyFrame : oldFrame, features, 500, 0.3, 5);
            // Convert result type
            oldFeatures = new MatOfPoint2f();
            features.convertTo(oldFeatures, CV_32FC2);

            if (feedbackMask == null)
                feedbackMask =  new Mat(feedbackImage.size(), feedbackImage.type(), new Scalar(0));
        }

        // Now track features into this frame
        if (oldFrame != null) {
            // Algorithm parameters
            Size winSize = new Size(15, 15);
            int maxLevel = 2;
            // Outputs
            MatOfByte status = new MatOfByte();
            MatOfFloat err = new MatOfFloat();
            MatOfPoint2f newFeatures = new MatOfPoint2f();
            // Calculate
            calcOpticalFlowPyrLK(oldFrame, greyFrame, oldFeatures, newFeatures, status, err, winSize, maxLevel);
//            TermCriteria termCriteria = new TermCriteria(TermCriteria.EPS | TermCriteria.COUNT, 10, 0.03);
//            calcOpticalFlowPyrLK(oldFrame, newFrame, oldFeatures, newFeatures, status, err, winSize, maxLevel, termCriteria, 0, 1);

            // Only use "good" points
            byte[] sta = status.toArray();
            Point[] olda = oldFeatures.toArray();
            Point[] newa = newFeatures.toArray();
//            List<Point> goodOld = new ArrayList<>();
            List<Point> goodNew = new ArrayList<>();
            for (int i = 0; i < sta.length; i++) {
                if (sta[i] == 1) {
//                    goodOld.add(olda[i]);
                    goodNew.add(newa[i]);
                    result.add(new MotionDetector.DetectedObject(newa[i]));

                    if (params.grParams.showFlow)
                        Imgproc.line(feedbackMask, olda[i], newa[i], idToRGB(i));
                }
            }
            if (params.grParams.showFlow)
                Core.add(feedbackMask, feedbackImage, feedbackImage);

            // Save new good features as old features for next iteration
            oldFeatures.fromList(goodNew);
        }

        // New frame becomes the old frame for the next iteration
        oldFrame = greyFrame.clone();

        return result;
    }

    @Override
    public String toString() {
        return "Sparse optical flow detector using Lucas-Kanade method with pyramids";
    }
}
