package com.jim.tracking.bg;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by jim_m on 14-May-17.
 */
public class AveragingBackground extends BackgroundHandler {

    public static void register() {
        BackgroundHandler.registerHandlerFactory("FirstFrames", arg -> {
            if (arg.userArgs.length != 1)
                throw new RuntimeException("Incorrect arguments for FirstFrames, required 1, found " + arg.userArgs.length);
            final long framesToAverage = Long.parseLong(arg.userArgs[0]);
            // Special case
            if (framesToAverage == 1)
                return new FirstFrame();
            return new AveragingBackground(framesToAverage, true);
        });
        BackgroundHandler.registerHandlerFactory("PreviousFrames", arg -> {
            final long framesToAverage = Long.parseLong(arg.userArgs[0]);
            // Special case
            if (framesToAverage == 1)
                return new PreviousFrame();
            return new AveragingBackground(framesToAverage, false);
        });
    }

    private long framesToAverage;
    private boolean useStartFrames;
    private long frameCount = 0;

    private AveragingBackground(long framesToAverage, boolean useStartFrames) {
        this.framesToAverage = framesToAverage;
        this.useStartFrames = useStartFrames;
    }

    @Override
    public Mat getBackground() {
        final Mat bg = super.getBackground();
        Mat r = null;
        if (bg != null) {
            r = new Mat();
            bg.convertTo(r, CvType.CV_8U);
        }
        return r;
    }

    @Override
    public void processFrame(Mat frame) {
        // If it's the first frame...
        if (background == null)
            // Create a zero Matrix
            background = Mat.zeros(frame.size(), CvType.CV_64F);

        if (!useStartFrames || frameCount < framesToAverage)
            // Add this frame to the background
            Imgproc.accumulateWeighted(frame, background, 1.0 / framesToAverage);
        frameCount++;
    }

    public static class FirstFrame extends BackgroundHandler {

        @Override
        public void processFrame(Mat frame) {
            if (background == null)
                background = frame.clone();
        }

    }

    public static class PreviousFrame extends FirstFrame {
        @Override
        public void processFrame(Mat frame) {
            background = frame.clone();
        }
    }
}
