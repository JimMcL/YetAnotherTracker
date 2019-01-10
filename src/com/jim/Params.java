package com.jim;

import com.jim.tracking.ForegroundSegmenter;
import com.jim.tracking.MotionDetector;
import com.jim.tracking.Region;
import com.jim.tracking.bg.BackgroundHandler;
import com.jim.ui.OptionalFeedbackWindow;
import com.jim.util.Dimension;
import com.jim.util.ObserverMgr;
import com.jim.util.Util;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jim_m on 29-May-17.
 */
@SuppressWarnings("WeakerAccess")
public class Params {

    public enum ThresholdType {OTSU, ADAPTIVE, GLOBAL}

    /** Options which describe the video input, and any transformations to be applied to it. */
    public static class SrcVideoParams {
        // Input video file name
        public String videoFile;
        // Rotation of input video
        public Util.OrthoRotation angle = Util.OrthoRotation.NONE;
        // View scale in pixels/unit AFTER RESIZING. Only affects coordinates in output CSV
        public double scale = Double.NaN;
        // Real world size of field of view. Only affects coordinates in output CSV
        public Dimension width;
        // Real world size of field of view. Only affects coordinates in output CSV
        public Dimension height;
        /** Width to resize input frame to - speeds up processing. 0 means don't resize. */
        public double resizeWidth = 0;

        /** Frame size after rotation and scaling. This is not really a parameter:
         * it is derived from other parameters and the input video size.
         * It is set the first time through the frame loop. */
        public Size frameSize;
        // True if the scale dialog should be displayed on startup
        public boolean manualScale;
        /** Input video recording frame rate (frames/sec). */
        public int fps;


        public Size scaleSize(Size originalFrameSize) {
            if (resizeWidth == 0)
                return originalFrameSize;
            return Util.setWidthPreservingAR(resizeWidth, originalFrameSize);
        }

        /** Returns the scale factor applied to convert the input video to the resized frame. */
        public double scale(Size originalFrameSize) {
            if (resizeWidth == 0)
                return 1;
            return resizeWidth / originalFrameSize.width;
        }

        public void setScale(double scale) {
            this.scale = scale;
            ObserverMgr.getInstance().fire(this);
        }

        /** Sets the frame size. */
        public void setFrameSize(Size frameSize) {
            this.frameSize = frameSize;
        }
    }

    /** Options which affect video playback, largely useful for debugging what is going on. */
    public static class GraphicParams {
        public boolean running = true;
        public boolean quitWhenDone = true;
        public boolean showWindow = true;
        /** True if feedback window should should the grey frame, otherwise the original (resized) frame is shown. */
        public boolean feedbackGrey = false;
        /** Display the result of background subtraction in a window. */
        public OptionalFeedbackWindow subtractionFeedback;
        /** Display the result of thresholding in a window. */
        public OptionalFeedbackWindow thresholdFeedback;
        /** Display background in a window. */
        public OptionalFeedbackWindow backgroundFeedback;
        public boolean showContours = false;
        public boolean showRectangle = false;
        public boolean showEllipse = false;
        public boolean showCentroid = false;
        public boolean showTracks = false;
        public double playbackSpeed = 1;
        public boolean rotateToTrack = false;
        public Size playbackSize = null;
        public boolean verbose = false;
        public boolean debug = true;
        public boolean showFeatures = true;
        public boolean showFlow = false;
    }

    /** Parameters which control moving object detection and object tracking. */
    public static class TrackerParams {
        public ForegroundSegmenter foregroundSegmenter;
        public BackgroundHandler backgroundHandler;
        /** Type of thresholding to apply. */
        public ThresholdType thresholdMethod = ThresholdType.ADAPTIVE;
        /** Should thresholding be inverted? */
        public boolean thresholdInvert = true;
        /** Value of block size to use when thresholding (see http://docs.opencv.org/3.2.0/d7/d1b/group__imgproc__misc.html#ga72b913f352e4a1b1b397736707afcde3) */
        public int thresholdBlockSize = 5;
        /** Value of C to use when thresholding (see http://docs.opencv.org/3.2.0/d7/d1b/group__imgproc__misc.html#ga72b913f352e4a1b1b397736707afcde3) */
        public double threshholdC = 2;
        /** Value of threshold to use when applying global thresholding (see http://docs.opencv.org/3.2.0/d7/d1b/group__imgproc__misc.html#gae8a4a146d1ca78c626a53577199e9c57) */
        public double threshold = 127;
        /** Area of the smallest contour of interest (units are pixels in resized frame) */
        public double minContourArea = 60;
        /** Area of the largest contour of interest (units are pixels in resized frame) */
        public double maxContourArea = 2000;
        /** Length of the smallest contour of interest (units are pixels in resized frame) */
        public double minContourLength = 0;
        /** Length of the largest contour of interest (units are pixels in resized frame) */
        public double maxContourLength = Double.MAX_VALUE;
        /** Maximum width/height of detected ellipse of interest */
        public double maxLength = 150;
        /** Sizes of ellipses to be applied as the structuring element for dilation (i.e. to expand foreground areas) or erosion. Negative values indicate erosion, positive are dilation. */
        public double[] dilationErosionSize = new double[0];
        /** Size of kernel when blurring image to reduce noise. */
        public int blurSize = 3;
        /** Equalize Y (luma) histogram) */
        public boolean equalize = false;

        /** Maximum size an object can shift in a single frame and still be part of the same track. */
        public double maxJump = 100;
        /** Objects closer than this without pre-existing tracks are merged into a single object. */
        public double minGap = 50;
        /** Tracks which stop moving within this distance of the border will be terminated. */
        public double terminationBorder = -1;
        /** Factor to apply (pixels/frame) when determining how to penalise old tracks when matching detected objects to tracks. */
        public double ageWeighting = 0;

        /** Region of interest - used to ignore extraneous movement etc.
         * This region is in untransformed source image coordinates. */
        private Region mask = null;
        public Region getMask() {
            return mask;
        }
        public synchronized void setMask(Region mask) {
            this.mask = mask;
            ObserverMgr.getInstance().fire(this);
        }
        public synchronized Region getTransformedMask() {
            return transformedMask;
        }
        public synchronized void setTransformedMask(Region transformedMask) {
            this.transformedMask = transformedMask;
        }

        /** Mask, transformed in the same way as the input video.
         * This is a hack, and doesn't really belong here,
         * but I can't decide where it should be. */
        private Region transformedMask;

        /** Units in output CSV file. Not actually used for now. */
        public String outputUnits;

        /** Optional filters, applied in order. */
        public List<MotionDetector.Filter> filters = new ArrayList<>();
        public MotionDetector.Detector detector;
        public boolean correlateObjectsWithFeatures = false;
    }

    public final SrcVideoParams srcParams = new SrcVideoParams();
    public final GraphicParams grParams = new GraphicParams();
    public final TrackerParams trParams = new TrackerParams();
}
