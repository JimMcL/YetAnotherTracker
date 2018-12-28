package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

public class MOGForegroundSegmenter implements ForegroundSegmenter {

    private final BackgroundSubtractorMOG2 subtractor;
    private Mat fgMask;

    public MOGForegroundSegmenter() {
        this(500, 16, true);
    }

    public MOGForegroundSegmenter(int history, int varThreshold, boolean detectShadows) {
        subtractor = Video.createBackgroundSubtractorMOG2(history, varThreshold, detectShadows);
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
                (int)Math.round(subtractor.getVarThreshold()) + ":" +
                subtractor.getDetectShadows();
    }

}
