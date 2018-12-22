package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

/** Detects foreground using background subtraction. */
public class BackgroundSubtractionSegmenter implements ForegroundSegmenter {
    private Mat foregroundFrame;

    @Override
    public Mat segment(int frameIndex, Mat greyFrame, Mat colourFrame, Params params) {
        if (foregroundFrame == null)
            foregroundFrame = greyFrame.clone();

        // Build difference frame
        // Determine pixels which changed from the background to this one
        final Mat bg = params.trParams.backgroundHandler.getBackground();
        if (bg != null) {
            params.grParams.backgroundFeedback.maybeShowFrame(bg);

            Core.absdiff(greyFrame, bg, foregroundFrame);
            params.grParams.subtractionFeedback.maybeShowFrame(foregroundFrame);

            int thresholdType = params.trParams.thresholdInvert ? THRESH_BINARY_INV : THRESH_BINARY;
            switch(params.trParams.thresholdMethod) {
                case GLOBAL:
                    Imgproc.threshold(foregroundFrame, foregroundFrame, params.trParams.threshold, 255, thresholdType);
                    break;
                case ADAPTIVE:
                    Imgproc.adaptiveThreshold(foregroundFrame, foregroundFrame, 255,
                            Imgproc.ADAPTIVE_THRESH_MEAN_C,
                            thresholdType, params.trParams.thresholdBlockSize, params.trParams.threshholdC);
                    break;
                case OTSU:
                    double value = Imgproc.threshold(foregroundFrame, foregroundFrame, 0, 255, thresholdType + THRESH_OTSU);
                    if (params.grParams.verbose)
                        System.out.println("Otsu thresholding value = " + value);
                    break;
            }
        }

        return foregroundFrame;
    }
}
