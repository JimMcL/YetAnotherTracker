package com.jim.tracking.bg;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/** A background handler which just creates a background with all pixels = 0.
 */
public class NoBackground extends BackgroundHandler {
    @Override
    public void processFrame(Mat frame) {
        if (background == null)
            // Create a zero Matrix
            background = Mat.zeros(frame.size(), CvType.CV_8UC1);
    }

    public static void register() {
        BackgroundHandler.registerHandlerFactory("none", arg -> new NoBackground());
    }
}
