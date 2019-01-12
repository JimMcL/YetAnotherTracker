package com.jim;

import com.jim.tracking.BackgroundSubtractionSegmenter;
import com.jim.tracking.DebugOverlay;
import com.jim.tracking.DifferenceDetector;
import com.jim.tracking.FeedbackTracker;
import com.jim.tracking.ForegroundSegmenter;
import com.jim.tracking.KNNForegroundSegmenter;
import com.jim.tracking.KalmanTrack;
import com.jim.tracking.MOGForegroundSegmenter;
import com.jim.tracking.MotionDetector;
import com.jim.tracking.MultiTracker;
import com.jim.tracking.OpticalFlowDetector;
import com.jim.tracking.Region;
import com.jim.tracking.TrackCSVWriter;
import com.jim.tracking.VideoResult;
import com.jim.tracking.bg.AveragingBackground;
import com.jim.tracking.bg.BackgroundHandler;
import com.jim.tracking.bg.FullMovie;
import com.jim.tracking.bg.NoBackground;
import com.jim.ui.OptionalFeedbackWindow;
import com.jim.util.Dimension;
import com.jim.util.ObserverMgr;
import com.jim.util.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.opencv.core.Core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Properties;

import static com.jim.util.Util.join;
import static com.jim.util.Util.parseSize;

@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
public class ParamsBuilder {
    private static final String DEFAULTS_SUFFIX = ".properties";

    public static Params build(String[] args) throws IOException {
        // Command line processing
        Params params = new Params();
        String bgDescr = "PreviousFrames:1";
        String motionDetector = "optical-flow";
        String fgSegmenter = "KNN";
        int firstTrackingFrame = 1;
        boolean checkForMask = false;

        Options options = new Options();

        // File containing options
        options.addOption(null, "defaults",true, "properties file containing command line defaults");

        // Options describing the input video
        options.addOption(null, "video",true, "input video file name");
        options.addOption(null, "fps",true, "input video frame rate");
        options.addOption(null, "view-rotation",true, "angle (deg) to rotate video before processing");
        options.addOption(null, "view-width",true, "real world field-of-view width (eg 600mm)");
        options.addOption(null, "view-height",true, "real world field-of-view height (eg 600mm)");
        options.addOption(null, "view-scale",true, "scale (after resizing) in pixels/<unit>, or else \"?\"");

        // Options controlling feedback while running
        options.addOption(null, "headless", false, "Run without user interface");
        options.addOption(null, "autorun", true, "Start playing video immediately");
        options.addOption(null, "exit-on-finish", true, "Exit when video has finished");
        options.addOption("v", "verbose",false, "verbose output");
        options.addOption("d", "debug",false, "very verbose output");
        options.addOption(null, "debug-overlay",false, "debug overlay");
        options.addOption(null, "display-grey", false, "show grey (and blurred) frames in video window");
        options.addOption(null, "display-background", false, "display the background in a window");
        options.addOption(null, "display-subtraction", false, "display the result of background subtraction in a window");
        options.addOption(null, "display-threshold", false, "display threshold video in a window");
        options.addOption(null, "display-flow", false, "display optical flow tracks");
        options.addOption("c", "display-contours", false, "display contours");
        options.addOption("r", "display-rectangle",false, "display bounding rectangle");
        options.addOption("p", false, "display centroid point");
        options.addOption("e", false, "display rotated ellipse");
        options.addOption(null, "display-tracks", false, "display symbols for tracks");
        options.addOption(null, "display-features", false, "display detected features");
        options.addOption("s", "playback-speed", true, "playback speed (default 1)");
        options.addOption("t", "track",false, "visually track object");
        options.addOption("a", false, "rotate window to tracking angle");
        options.addOption(null, "frame-size", true, "output frame size (default <width>x<height>, default same as input video after resizing)");

        // Options controlling moving object detection

        options.addOption(null, "motion-detector", true, "motion detector (optical-flow or differences, default " + motionDetector + ")");
        options.addOption(null, "foreground-segmenter", true, "foreground segmenter (background-subtraction, KNN or MOG, default " + fgSegmenter + ")");
        options.addOption(null, "resize", true, "resize input video to this width before processing (pixels)");
        options.addOption(null, "equalize", false, "equalize histogram on input video before processing");
        options.addOption(null, "blur-size", true, "kernel size for gaussian blur (pixels, default " + params.trParams.blurSize + ")");
        options.addOption(null, "threshold-method", true, "type of thresholding to apply (" + join(Params.ThresholdType.values(), ", ")+ ", default " + params.trParams.thresholdMethod + ")");
        options.addOption(null,  "threshold-invert", true,"if true, dark areas are treated as foreground (default " + params.trParams.thresholdInvert + ")");
        options.addOption(null, "threshold-C", true, "constant to use for adaptive thresholding (default " + params.trParams.threshholdC + ")");
        options.addOption(null, "threshold-blocksize", true, "block size to use for adaptive thresholding (must be odd, > 1, default " + params.trParams.thresholdBlockSize + ")");
        options.addOption(null, "threshold", true, "threshold value to use for global thresholding (1 - 254, default " + params.trParams.threshold + ")");
        options.addOption(null, "dilation-erosion", true, "comma-separated list of dilation (+ve) or erosion (-ve) commands (default " + Util.join(params.trParams.dilationErosionSize, ",") + ")");
        options.addOption(null, "min-contour", true, "minimum contour area to track (default " + params.trParams.minContourArea + ")");
        options.addOption(null, "min-contour-length", true, "minimum length of contour to track (default " + params.trParams.minContourLength + ")");
        options.addOption(null, "max-contour", true, "maximum contour area to track (default " + params.trParams.maxContourArea + ")");
        options.addOption(null, "max-contour-length", true, "maximum length of contour to track (default " + params.trParams.maxContourLength + ")");
        options.addOption(null, "max-length", true, "maximum width/height of ellipse to track (default " + params.trParams.maxLength + ")");
        options.addOption(null, "max-jump", true, "maximum distance between detections which can belong to the same track (default " + params.trParams.maxJump + ")");
        options.addOption(null, "min-gap", true, "multiple objects closer than this will not create new tracks (default " + params.trParams.minGap + ")");
        options.addOption(null, "age-weighting", true, "importance of track age when assigning tracks to detected objects (pixels/frame, default " + params.trParams.ageWeighting + ")");
        options.addOption(null, "background-method", true, "background calculation method (default " + bgDescr + ")");
        options.addOption(null, "mask-file", true, "JSON file defining region of interest");
        options.addOption(null, "mask", true, "If true and <filename>.json file exists, it is used as a mask file name (default " + checkForMask + ")");
        options.addOption(null, "termination-border", true, "Tracks which stop moving within this distance of the border will be terminated (default not terminated)");

        // Object tracking options
        options.addOption("k", "kalman",true, "object tracking using a kalman filter");
        options.addOption(null, "first-tracking-frame",true, "first frame to calculate tracks for (default " + firstTrackingFrame + ")");
        options.addOption(null, "no-new-tracks",false, "if specified, tracks are only created in the first frame");

        // Output file options
        options.addOption("o",  "output", true, "output CSV or video file name");
        options.addOption(null, "csv", false, "writes a CSV file with name based on the video file name");
        options.addOption(null, "csv-comment-prefix", true, "prefix for comments in CSV file - runtime parameters will be written to the file");
        options.addOption(null, "csv-units", true, "units to output points in, either pixels or view-width/view-height units (default units of view-width/-height)");
        options.addOption(null, "output-all-frames", false, "write duplicated consecutive track positions to CSV file");

        // Note I am using DefaultParser rather the CommandLineParser so I can use the properties file defaults (see parse below)
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            // If a defaults file has been specified...
            if(cmd.hasOption("defaults")) {
                // Re-parse using the specified file
                try {
                    Properties defaults = readProperties(cmd.getOptionValue("defaults"), cmd.hasOption("v"));
                    cmd = parser.parse(options, args, defaults);
                } catch (UnrecognizedOptionException e) {
                    System.err.println("Unable to read defaults file: unrecognized option '" + e.getOption() + "'");
                    printUsageAndExit(options);
                } catch (Exception e) {
                    System.err.println("Unable to read defaults file: " + e.getLocalizedMessage());
                    printUsageAndExit(options);
                }
            }
        } catch (ParseException e) {
            System.err.println("Invalid command line options: " + e.getLocalizedMessage());
            printUsageAndExit(options);
        }

        @SuppressWarnings("ConstantConditions") String[] posArgs = cmd.getArgs();
        if(cmd.hasOption("v"))
            params.grParams.verbose = true;
        params.grParams.showWindow = !cmd.hasOption("headless");
        params.grParams.running = booleanArg(cmd, "autorun", true);
        params.grParams.quitWhenDone = booleanArg(cmd, "exit-on-finish", false);
        params.grParams.debug = cmd.hasOption("d");
        params.srcParams.videoFile = stringArg(cmd, "video");
        params.srcParams.fps = intArg(cmd, "fps", 0);
        params.srcParams.angle = orthoRotateArg(cmd, "view-rotation");
        params.srcParams.width = dimensionArg(cmd, "view-width");
        params.srcParams.height = dimensionArg(cmd, "view-height");
        if ("?".equals(stringArg(cmd, "view-scale"))) {
            params.srcParams.manualScale = true;
            // Force autorun off
            params.grParams.running = false;
        } else
            params.srcParams.scale = doubleArg(cmd, "view-scale", 0);
        params.grParams.feedbackGrey = cmd.hasOption("display-grey");
        params.grParams.backgroundFeedback = new OptionalFeedbackWindow("Background", cmd.hasOption("display-background"));
        params.grParams.thresholdFeedback = new OptionalFeedbackWindow("Threshold", cmd.hasOption("display-threshold"));
        params.grParams.subtractionFeedback = new OptionalFeedbackWindow("Subtraction", cmd.hasOption("display-subtraction"));
        if(cmd.hasOption("c"))
            params.grParams.showContours = true;
        if(cmd.hasOption("r"))
            params.grParams.showRectangle = true;
        if(cmd.hasOption("p"))
            params.grParams.showCentroid = true;
        if(cmd.hasOption("e"))
            params.grParams.showEllipse = true;
        params.grParams.showTracks = cmd.hasOption("display-tracks");
        params.grParams.showFeatures = cmd.hasOption("display-features");
        params.grParams.showFlow = cmd.hasOption("display-flow");
        if(cmd.hasOption("background-method"))
            bgDescr = cmd.getOptionValue("background-method");
        if (cmd.hasOption("frame-size"))
            params.grParams.playbackSize = parseSize(cmd.getOptionValue("frame-size"));
        params.grParams.playbackSpeed = doubleArg(cmd, "s", params.grParams.playbackSpeed);
        params.srcParams.resizeWidth = doubleArg(cmd, "resize", params.srcParams.resizeWidth);
        params.trParams.equalize = cmd.hasOption("equalize");
        params.trParams.thresholdMethod = Params.ThresholdType.valueOf(stringArg(cmd,"threshold-method", params.trParams.thresholdMethod.toString()).toUpperCase());
        params.trParams.thresholdInvert = booleanArg(cmd, "threshold-invert", params.trParams.thresholdInvert);
        params.trParams.threshholdC = doubleArg(cmd, "threshold-C", params.trParams.threshholdC);
        params.trParams.thresholdBlockSize = intArg(cmd, "threshold-blocksize", params.trParams.thresholdBlockSize);
        params.trParams.threshold = doubleArg(cmd, "threshold", params.trParams.threshold);
        params.trParams.dilationErosionSize = doubleArrayArg(cmd, "dilation-erosion", params.trParams.dilationErosionSize);
        params.trParams.blurSize = (int) doubleArg(cmd, "blur-size", params.trParams.blurSize);
        params.trParams.minContourArea = doubleArg(cmd, "min-contour", params.trParams.minContourArea);
        params.trParams.maxContourArea = doubleArg(cmd, "max-contour", params.trParams.maxContourArea);
        params.trParams.minContourLength = doubleArg(cmd, "min-contour-length", params.trParams.minContourLength);
        params.trParams.maxContourLength = doubleArg(cmd, "max-contour-length", params.trParams.maxContourLength);
        params.trParams.maxLength = doubleArg(cmd, "max-length", params.trParams.maxLength);
        params.trParams.maxJump = doubleArg(cmd, "max-jump", params.trParams.maxJump);
        params.trParams.minGap = doubleArg(cmd, "min-gap", params.trParams.minGap);
        params.trParams.ageWeighting = doubleArg(cmd, "age-weighting", params.trParams.ageWeighting);
        params.trParams.terminationBorder = doubleArg(cmd, "termination-border", params.trParams.terminationBorder);

        // Filters (order is important)
        final boolean hasKalmanTracker = cmd.hasOption("k");
        if(hasKalmanTracker) {
            firstTrackingFrame = intArg(cmd, "first-tracking-frame", firstTrackingFrame);
            boolean allowNewTracks = !cmd.hasOption("no-new-tracks");
            params.trParams.filters.add(new MultiTracker(new KalmanTrack.Cfg(cmd.getOptionValue("k")), firstTrackingFrame, allowNewTracks));
        }
        if(cmd.hasOption("t"))
            params.trParams.filters.add(new FeedbackTracker());
        params.grParams.rotateToTrack = cmd.hasOption("a");

        // Motion detector
        if (cmd.hasOption("motion-detector"))
            motionDetector = cmd.getOptionValue("motion-detector");
        params.trParams.detector = detectorFromName(motionDetector);
        params.trParams.foregroundSegmenter = fgSegmenterFromName(stringArg(cmd, "foreground-segmenter", fgSegmenter));
        if (params.trParams.detector == null || params.trParams.foregroundSegmenter == null) {
//            printUsageAndExit(options);
            System.exit(1);
        }

        // Output - can output either CVS file or video which tracks moving object
        TrackCSVWriter writer = null;
        if(cmd.hasOption("output")) {
            for (String fileName : cmd.getOptionValues("o")) {
                // Does it look like a CSV file name?
                if (TrackCSVWriter.canHandle(fileName)) {
                    writer = getTrackCSVWriter(params, cmd, hasKalmanTracker, fileName);
                } else {
                    // Assume it's a video file
                    params.trParams.filters.add(new VideoResult(fileName, params.grParams));
                }
            }
        }

        // Assume single positional argument is video file name
        if (params.srcParams.videoFile == null && posArgs.length == 1) {
            params.srcParams.videoFile = posArgs[0];
        }
        if (params.srcParams.videoFile == null) {
            System.err.println("Missing video file name");
            printUsageAndExit(options);
        }
        if (!new File(params.srcParams.videoFile).canRead()) {
            System.err.println("Unable to read video file '" + params.srcParams.videoFile + "'");
            printUsageAndExit(options);
        }
        if (posArgs.length > 1) {
            System.err.println("Too many arguments " + posArgs.length + ", only 1 video may be specified");
            printUsageAndExit(options);
        }

        maybeSetMask(cmd, params.srcParams.videoFile, checkForMask, params);

        if(cmd.hasOption("csv") && writer == null)
            writer = getTrackCSVWriter(params, cmd, hasKalmanTracker, TrackCSVWriter.deriveName(params.srcParams.videoFile));

        if(cmd.hasOption("debug-overlay"))
            params.trParams.filters.add(new DebugOverlay());

        if (params.grParams.verbose) {
            System.out.println("OpenCV version " + Core.VERSION);
            System.out.println("Motion detector " + params.trParams.detector);
            System.out.println("Foreground segmenter " + params.trParams.foregroundSegmenter);
            if (params.trParams.getMask() == null)
                System.out.println("No mask");
            else
                System.out.println("Mask bounds " + params.trParams.getMask());
            System.out.println("Applying filters:");
            for (MotionDetector.Filter filter : params.trParams.filters) {
                System.out.println("    " + filter);
            }
        }

        // Register available background handlers
        AveragingBackground.register();
        FullMovie.register();
        NoBackground.register();    // Allow no background, in which case the original image is segmented without first subtracting background
        params.trParams.backgroundHandler = BackgroundHandler.getHandler(bgDescr, params.srcParams.videoFile, params);
        if (params.grParams.verbose) {
            System.out.println("Background handler = " + params.trParams.backgroundHandler);
        }

        // Optionally add runtime parameters to CSV file as a comment
        if (cmd.hasOption("csv-comment-prefix") && writer != null) {
            writer.setCommentPrefix(cmd.getOptionValue("csv-comment-prefix"));
            writer.writeComment(params.srcParams.videoFile);
            writer.writeComment(Util.join(args, " "));
        }

        return params;
    }

    // =================================================================
    // Private methods

    private static void printUsageAndExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "SpiderTracker", options);
        System.exit(1);
    }

    private static Properties readProperties(String name, boolean verbose) throws IOException {
        Properties props = new Properties();
        try (InputStream is = openProperties(name, verbose)) {
            props.load(is);
        }
        return props;
    }

    private static InputStream openProperties(String name, boolean verbose) throws IOException {
        // Look for local file first, then a resource
        File file = findFile(new File(name), DEFAULTS_SUFFIX);
        if (file != null) {
            if (verbose)
                System.out.println("Using defaults from file " + file);
            return new FileInputStream(file);
        }
        final ClassLoader classLoader = ParamsBuilder.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(name);
        if (is == null)
            is = classLoader.getResourceAsStream(name + DEFAULTS_SUFFIX);
        if (is == null)
            throw new RuntimeException("Unable to locate properties '" + name + '"');
        if (verbose)
            System.out.println("Using defaults from builtin resource '" + name + "'");

        return is;
    }

    private static File findFile(File file, String suffix) {
        // Can the file as specified be accessed
        if (file.canRead())
            return file;

        // Try appending the suffix
        Path path = file.toPath();
        path = path.resolveSibling(path.getFileName() + suffix);
        file = path.toFile();
        if (file.canRead())
            return file;

        return null;
    }

    private static String stringArg(CommandLine cmd, String optName) {
        return stringArg(cmd, optName, null);
    }

    private static String stringArg(CommandLine cmd, String optName, String defaultValue) {
        String value = defaultValue;
        if (cmd.hasOption(optName)) {
            value = cmd.getOptionValue(optName);
        }
        return value;
    }

    private static double doubleArg(CommandLine cmd, String optName, double defaultValue) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName);
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value (" + value + ") for option " + optName);
            }
        }
        return defaultValue;
    }

    private static double[] doubleArrayArg(CommandLine cmd, String optName, double[] defaultValue) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName);
            try {
                String[] parts = value.split(",");
                double[] result = new double[parts.length];
                for (int i = 0; i < result.length; i++) {
                    result[i] = Double.parseDouble(parts[i]);
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Invalid comma-separated list (" + value + ") for option " + optName);
            }
        }
        return defaultValue;
    }

    private static int intArg(CommandLine cmd, String optName, int defaultValue) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName);
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value (" + value + ") for option " + optName);
            }
        }
        return defaultValue;
    }

    private static Dimension dimensionArg(CommandLine cmd, String optName) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName);
            try {
                return Dimension.parse(value);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value (" + value + ") for option " + optName);
            }
        }
        return null;
    }

    private static boolean booleanArg(CommandLine cmd, String optName, boolean defaultValue) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName);
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                throw new RuntimeException("Invalid value (" + value + ") for option " + optName, e);
            }
        }
        return defaultValue;
    }

    private static Util.OrthoRotation orthoRotateArg(CommandLine cmd, String optName) {
        if (cmd.hasOption(optName)) {
            final String value = cmd.getOptionValue(optName).trim();
            try {
                return Util.OrthoRotation.fromAngle(Integer.parseInt(value));
            } catch (Exception e) {
                throw new RuntimeException("Invalid value for option " + optName, e);
            }
        }
        return Util.OrthoRotation.NONE;
    }

    private static MotionDetector.Detector detectorFromName(String motionDetector) {
        if (motionDetector.equals("differences"))
            return new DifferenceDetector();
        else if (motionDetector.equals("optical-flow"))
            return new OpticalFlowDetector();
        System.err.println("Invalid motion detector '" + motionDetector + "', require one of (differences, optical-flow)");
        return null;
    }

    private static ForegroundSegmenter fgSegmenterFromName(String segmenter) {
        String[] parts = segmenter.split(":");
        String name = parts[0];

        switch (name) {
            case "background-subtraction":
                return new BackgroundSubtractionSegmenter();
            case "MOG":
                if (parts.length == 1)
                    return new MOGForegroundSegmenter();
                else {
                    if (parts.length == 4)
                        return new MOGForegroundSegmenter(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Boolean.parseBoolean(parts[3]));
                }
                break;
            case "KNN":
                if (parts.length == 1)
                    return new KNNForegroundSegmenter();
                else {
                    if (parts.length == 4)
                        return new KNNForegroundSegmenter(Integer.parseInt(parts[1]), Double.parseDouble(parts[2]), Boolean.parseBoolean(parts[3]));
                }
                break;
        }
        System.err.println("Invalid foreground segmenter '" + name + "', require one of " +
                "(\n\tbackground-subtraction, \n\tMOG[:history:varThreshold:detectShadows]), " +
                "\n\tKNN[:history:dist2Threshold:detectShadows])");
        return null;
    }

    private static void maybeSetMask(CommandLine cmd, String videoFile, boolean checkForMask, Params params) {
        boolean onlyIfExists = booleanArg(cmd, "mask", checkForMask);
        System.out.println("onlyIfExists = " + onlyIfExists);
        System.out.println("cmd.hasOption(optName) = " + cmd.hasOption("mask"));
        System.out.println("cmd.getOptionValue(optName) = " + cmd.getOptionValue("mask"));
        String fileName;
        if (cmd.hasOption("mask-file")) {
            fileName = cmd.getOptionValue("mask-file");
            if (!new File(fileName).exists()) {
                System.err.println("mask-file does not exist: " + fileName);
                System.exit(1);
            }
        } else if (onlyIfExists) {
            fileName = Util.replaceExtension(videoFile, ".json");
            System.out.println("Checking existence of mask file " + fileName);
            if (!new File(fileName).exists()) {
                // Silently return
                System.out.println("Doesn't exist");
                return;
            }
        } else {
            return;
        }
        try {
            System.out.println("mask fileName = " + fileName);
            params.trParams.setMask(new Region(new FileReader(fileName)));
        } catch (IOException e) {
            System.err.println("Error reading " + fileName + ": " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static TrackCSVWriter getTrackCSVWriter(Params params, CommandLine cmd, boolean hasKalmanTracker, String fileName) {
        ChangeableCsvWriter writer = null;
        try {
            writer = new ChangeableCsvWriter(new FileWriter(fileName), cmd.hasOption("output-all-frames"), params.srcParams.fps) {
                @Override
                public void notify(Object object) {
                    Params.SrcVideoParams srcParams = (Params.SrcVideoParams) object;
                    setScale(srcParams.scale);
                }
            };
            writer.setWriteTracks(hasKalmanTracker);

            // Handle optional output units. Default to units in user specified view width or height, otherwise fallback to pixels
            String widthUnits = params.srcParams.width != null ? params.srcParams.width.getUnits() : null;
            String heightUnits = params.srcParams.height!= null ? params.srcParams.height.getUnits() : null;
            boolean useWidthUnits = widthUnits != null;
            boolean useHeightUnits = heightUnits != null;
            if (cmd.hasOption("csv-units")) {
                params.trParams.outputUnits = cmd.getOptionValue("csv-units");
                writer.setOutputUnitsName(params.trParams.outputUnits);
                // Units must be either the same as the units of a user specified view width/height or else pixels
                /*if (units.equals(widthUnits))
                    useWidthUnits = true;
                else if (units.equals(heightUnits))
                    useHeightUnits = true;
                else*/ if ("px".equals(params.trParams.outputUnits) || "pixels".equals(params.trParams.outputUnits))
                    useWidthUnits = useHeightUnits = false;
                else if (Double.isNaN(params.srcParams.scale))
                    throw new RuntimeException("Invalid csv units (" + params.trParams.outputUnits + "), must use the same units as view width or height, or else px");
            }
            if (params.srcParams.scale > 0) {
                if (useWidthUnits || useHeightUnits)
                    throw new RuntimeException("Only one of view-scale, view-width and view-height may be specified");
                writer.setScale(params.srcParams.scale);
            }
            if (useWidthUnits)
                writer.setUserUnitsWidth(params.srcParams.width.getDistance());
            else if (useHeightUnits)
                writer.setUserUnitsHeight(params.srcParams.height.getDistance());

            params.trParams.filters.add(writer);

            // Listen for changes to scale
            ObserverMgr.getInstance().observe(params.srcParams, writer);

        } catch (IOException e) {
            System.err.println("Unable to open output CSVWriter file " + fileName + ": " + e.getLocalizedMessage());
            System.exit(1);
        }
        return writer;
    }
}

/** A TrackCsvWriter which is an Observer. */
abstract class ChangeableCsvWriter extends TrackCSVWriter implements ObserverMgr.Observer {
    ChangeableCsvWriter(Writer writer, boolean writeAllFrames, int fps) throws IOException {
        super(writer, writeAllFrames, fps);
    }
}
