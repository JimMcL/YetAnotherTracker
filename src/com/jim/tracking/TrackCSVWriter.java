package com.jim.tracking;

import com.jim.Params;
import com.jim.util.Util;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jim_m on 13-May-17.
 */
public class TrackCSVWriter extends CSVWriter implements MotionDetector.Filter {

    private static final String CSV = ".csv";
    private boolean writeAllFrames;
    private int fps;
    private static final String[] HEADERS = {"Frame", "Time", "TrackId", "x", "y", "ValueChanged"};
    private boolean writeTracks;
    // Keep track of frames which were skipped in output
    private Map<Long, Point> skippedPos = new HashMap<>();
    private Size frameSize = null;
    private int lastFrameIndex;
    // For converting from pixels to user coordinates - size of frame in user units
    private double userUnitsWidth = 0;
    private double userUnitsHeight = 0;
    private double scaleFactor;
    @SuppressWarnings("FieldCanBeLocal")
    private String outputUnitsName;
    private double scale;

    public TrackCSVWriter(Writer writer, boolean writeAllFrames, int fps) throws IOException {
        super(writer);
        this.writeAllFrames = writeAllFrames;
        this.fps = fps;
        writeHeaders(HEADERS);
    }

    public void setWriteTracks(boolean writeTracks) {
        this.writeTracks = writeTracks;
    }

    @Override
    public String toString() {
        return Util.getFirstNamedAncestor(getClass()).getSimpleName() + " - writes tracked objects to a CSV file";
    }

    public static boolean canHandle(String fileName) {
        return fileName.toLowerCase().endsWith(CSV);
    }

    /** Sets the name of the spatial units written to the CSV file. The value is not currently used. */
    public void setOutputUnitsName(String outputUnitsName) {
        this.outputUnitsName = outputUnitsName;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    /** Defines the video scale based on real world width of the video frame. */
    public void setUserUnitsWidth(double userUnitsWidth) {
        this.userUnitsWidth = userUnitsWidth;
    }

    /** Defines the video scale based on real world height of the video frame. */
    public void setUserUnitsHeight(double userUnitsHeight) {
        this.userUnitsHeight = userUnitsHeight;
    }

    // Filter methods

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {
        if (!opts.grParams.running)
            return;

        int frameIndex = camera.getFrameIndex();
        frameSize = opts.grParams.playbackSize;
        lastFrameIndex = frameIndex;

        scaleFactor = scale > 0 ? 1 / scale :
                userUnitsWidth > 0 ? userUnitsWidth / frameSize.width :
                        userUnitsHeight > 0 ? userUnitsHeight / frameSize.height :
                                1;

        try {
            if (writeTracks) {
                for (KalmanTrack track : tracks) {
                    final long trackId = track.getTrackId();
                    final boolean valueChanged = track.isPositionChanged();
                    if (writeAllFrames || valueChanged) {
                        // When skipping frames, only the first and last frame at any position are written.
                        // Have to write the last frame retrospectively once it is known
                        Point lastPos = skippedPos.get(trackId);
                        if (lastPos != null) {
                            // Write the last frame
                            writePos(frameIndex, camera, trackId, lastPos, false);
                            skippedPos.remove(trackId);
                        }

                        // Write this frame
                        final Point pos = track.getCurrentPosition();
                        writePos(frameIndex, camera, trackId, pos, valueChanged);
                    } else {
                        // Save the position so that the last frame can be written out
                        if (!writeAllFrames)
                            skippedPos.put(trackId, track.getCurrentPosition());
                    }
                }
            } else if (detectedObjects.size() == 1) {
                for (MotionDetector.DetectedObject object : detectedObjects) {
                    // Only writing a single track, so give it id 1
                    writePos(frameIndex, camera, 1, object.centroid, true);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writePos(int frameIndex, VideoPlayer camera, long trackId, Point point, boolean valueChanged) throws IOException {
        // Convert frame index to time in seconds, using either user specific fps or the fps from the video
        double fps = this.fps > 0 ? this.fps : camera.getFps();
        // Convert point to user coordinates
        writeValues(new Object[]{frameIndex - 1, (frameIndex - 1) / fps, trackId, point.x * scaleFactor, point.y * scaleFactor, valueChanged});
    }

    @Override
    public void onCameraOpened(VideoPlayer camera) {
        // Calculate output units conversion
    }

    @Override
    public void onDone(VideoPlayer camera) {
        try {
            // Note that we don't want to write a final frame for stopped tracks,
            // but write a final frame with special ID -1 so that total number of frames
            // (and output frame size) can be determined, even if nothing is moving at the end
            if (frameSize != null)
                writePos(lastFrameIndex, camera, -1, new Point(frameSize.width, frameSize.height), false);

            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Given the name of a video file, returns the name of a CSV file with the same base name. */
    public static String deriveName(String videoFile) {
        return Util.replaceExtension(videoFile, CSV);
    }
}
