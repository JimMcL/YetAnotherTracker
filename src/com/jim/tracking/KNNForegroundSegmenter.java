package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractorKNN;
import org.opencv.video.Video;

public class KNNForegroundSegmenter implements ForegroundSegmenter {

    private final BackgroundSubtractorKNN subtractor;
    private Mat fgMask;

    public KNNForegroundSegmenter() {
        this(500, 400, true);
    }

    public KNNForegroundSegmenter(int history, double dist2Threshold, boolean detectShadows) {
        subtractor = Video.createBackgroundSubtractorKNN(history, dist2Threshold, detectShadows);
        subtractor.setShadowValue(0);
    }

    @Override
    public Mat segment(int frameIndex, Mat greyFrame, Mat colourFrame, Params params) {
        // If it's the first frame...
        if (fgMask == null)
            // Create a zero Matrix
            fgMask = Mat.zeros(colourFrame.size(), CvType.CV_64F);

        subtractor.apply(colourFrame, fgMask);
        return fgMask;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" +
                subtractor.getHistory() + ":" +
                (int)Math.round(subtractor.getDist2Threshold()) + ":" +
                subtractor.getDetectShadows();
    }
}
