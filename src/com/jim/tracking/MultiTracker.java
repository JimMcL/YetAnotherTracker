package com.jim.tracking;

import com.jim.Params;
import com.jim.util.Util;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for assigning detected objects to tracks.
 *
 * Algorithm:
 * Have list of existing tracks T, list of objects detected in the current frame O
 * For each track t, find closest object o
 * For each object o, find closest track t
 * TODO complete doco
 */
public class MultiTracker implements MotionDetector.Filter {

    private static long nextObjectId = 0;
    private KalmanTrack.Cfg cfg;
    private int firstFrameToProcess;
    private boolean allowNewTracks;
    private List<TrackWithEllipse> trackingObjects = new ArrayList<>();

    private static final int TR_UNASSIGNED = -1;
    private static final int TR_STOPPED = TR_UNASSIGNED;
    private static final int OBJ_UNASSIGNED = -1;
    private static final int OBJ_NO_TRACK = -2;
    private static final int OBJ_NEW_TRACK = OBJ_UNASSIGNED;

    /**
     *
     * @param cfg Kalman filter configuration.
     * @param firstFrameToProcess No tracks will be created before this frame.
     * @param allowNewTracks If true, new tracks can be created at any time.
     *                       If false, tracks can only be created in the first frame.
     */
    public MultiTracker(KalmanTrack.Cfg cfg, int firstFrameToProcess, boolean allowNewTracks) {
        this.cfg = cfg;
        this.firstFrameToProcess = firstFrameToProcess - 1; // Convert 1-based index to 0-based
        this.allowNewTracks = allowNewTracks;
    }

    // =========================================================================
    // Overrides

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {

        if (camera.getFrameIndex() >= firstFrameToProcess) {
            // Try to match detected objects to existing tracked objects
            trackingObjects = trackMulti(detectedObjects, trackingObjects, opts, allowNewTracks || camera.getFrameIndex() == firstFrameToProcess, greyFrame.size(), camera.getFrameIndex());
            tracks.addAll(trackingObjects);

            if (opts.grParams.showTracks) {
                for (TrackWithEllipse track : trackingObjects) {
                    Imgproc.drawMarker(feedbackImage, track.getCurrentPosition(), Util.idToRGB(track.getTrackId() + 1));
                }
            }
        }
    }

    @Override
    public void onCameraOpened(VideoPlayer camera) {
    }

    @Override
    public void onDone(VideoPlayer camera) {
    }

    @Override
    public String toString() {
        return "Kalman filter (" + cfg + ") - tracks object across frames";
    }

    // ==============================================================================================
    // Private

    /** Returns a list of tracks corresponding to the detected objects.
     *
     * @param detectedObjects Objects detected by motion detection.
     * @param tracks Pre-existing tracks.
     * @param params User specified parameter values.
     * @param allowNewTracks If false, tracks can only be created in the starting frame.
     * @param imageSize Size of the image being analysed.
     * @param frameNumber Number of this frame.
     * @return new list of tracks.
     */
    private List<TrackWithEllipse> trackMulti(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Params params, boolean allowNewTracks, Size imageSize, int frameNumber) {

        // Calculate distance between every pair of detected objects and existing tracks
        final int numTracks = tracks.size();
        final int numObjects = detectedObjects.size();
        TrackWithEllipse[] tracksArr = tracks.toArray(new TrackWithEllipse[numTracks]);
        MotionDetector.DetectedObject[] objectsArr = detectedObjects.toArray(new MotionDetector.DetectedObject[numObjects]);
        double[][] distances = buildDistanceMatrix(tracksArr, objectsArr, 0, false);
        // Get vector times since last detection for tracks. This is so we can prioritise newer tracks
        double[] trackAges = buildTimesVector(tracksArr, params.trParams.ageWeighting, frameNumber);

        // Build matrix of distances between every pair of objects
        double[][] objDistances = new double[numObjects][numObjects];
        for (int i = 0; i < numObjects - 1; i++) {
            for (int j = 1; j < numObjects; j++) {
                objDistances[i][j] = objDistances[j][i] = Util.distance(objectsArr[i].centroid, objectsArr[j].centroid);
            }
        }
        // We need:
        //   for each track, either an object or nothing (which indicates it has stopped)
        //   for each object, either an existing track, a new track, or nothing (which indicates it is a spurious object)
        int[] trackActions;
        int[] objActions;
        if (numObjects > 0) {
            int[][] assignments = assignTracksAndObjects(distances, objDistances, trackAges, params.trParams.maxJump, params.trParams.minGap);
            trackActions = assignments[0];
            objActions = assignments[1];
        } else {
            // All tracks are stopped
            trackActions = new int[numTracks];
            Arrays.fill(trackActions, TR_STOPPED);
            // No objects
            objActions = new int[0];
        }

        // Handle existing tracks
        List<TrackWithEllipse> result = new ArrayList<>();
        for (int i = 0; i < numTracks; i++) {
            boolean add = true;
            TrackWithEllipse track = tracksArr[i];
            int action = trackActions[i];
            // Each existing track is either assigned to an object or stopped
            if (action == TR_STOPPED) {
                // Optionally terminate a track if it goes outside the region.
                // The region is either the mask if it exists, or else just the frame bounds
                Region mask = params.trParams.getTransformedMask();
                // If no mask, use image bounds
                if (mask == null)
                    mask = new Region(true, new Rect(new Point(), imageSize));
                if (params.trParams.terminationBorder > 0 &&
                        mask.pointInside(track.getLastPredictedPoint()) < params.trParams.terminationBorder) {
                    add = false;
                }
                track.stopped();
            } else {
                track.apply(objectsArr[action].currentPos());
                track.setLastDetectedAt(frameNumber);
            }
            if (add)
                result.add(track);
        }

        // Create any new tracks
        if (allowNewTracks) {
            for (int objIdx : objActions) {
                        MotionDetector.DetectedObject obj = objectsArr[objIdx];
                        TrackWithEllipse track = new TrackWithEllipse(nextObjectId++, cfg, obj.centroid, obj.ellipse, frameNumber);
                        track.apply(obj.currentPos());
                        result.add(track);
                    }
        }

//        System.out.println(numObjects + " objects -> tracks: " + Util.join(result, ", "));

        return result;
    }

    /**
     *
     * @param distances matrix of distances from objects to tracks.
     * @param objectDistances matrix of distances between every pair of detected objects.
     * @param trackAges Weighted age (time since last detection) of each track.
     * @param maxJump maximum distance in a single step allowed within a track.
     * @param minGap duplicate objects closer than this are ignored.
     * @return assignments - value[0] array of tracks to object indices, value[1] is array of objects which require new tracks.
     */
    private int[][] assignTracksAndObjects(double[][] distances, double[][] objectDistances, double[] trackAges, double maxJump, double minGap) {

        final int numObjects = distances.length;
        final int numTracks = distances[0].length;

        // Handle simple case first - tracks which have an unambiguous best object
        int[] assignedTrackToObject = new int[numTracks];
        int[] assignedObjectToTrack = new int[numObjects];
        Arrays.fill(assignedTrackToObject, TR_UNASSIGNED);
        Arrays.fill(assignedObjectToTrack, OBJ_UNASSIGNED);
        int[] bestTrackToObject = new int[numTracks];
        int[] bestObjectToTrack = new int[numObjects];
        simpleAssignments(distances, trackAges, maxJump, assignedTrackToObject, assignedObjectToTrack, bestTrackToObject, bestObjectToTrack);

        // Now solve problems
        // Look for objects which may be artifacts of the process - i.e. very close together, and discard them
        for (int obj = 0; obj < bestObjectToTrack.length; obj++) {
            if (bestObjectToTrack[obj] < 0)
                continue;
            final int bestObj = bestTrackToObject[bestObjectToTrack[obj]];
            if (bestObj != obj) {
                // Get distance between this object and the one which was assigned to the best track
                double objDist = objectDistances[obj][bestObj];
//                System.out.println("objDist = " + objDist + ", " + obj + "-" + bestObj);
                if (objDist < minGap) {
                    assignedObjectToTrack[obj] = OBJ_NO_TRACK;
                    fillRow(distances, obj, Double.MAX_VALUE);
                }
            }
        }

        // Now look for tracks which are close enough to objects. Keep doing it until no more assignments can be made
        //noinspection StatementWithEmptyBody
        while (simpleAssignments(distances, trackAges, maxJump, assignedTrackToObject, assignedObjectToTrack, bestTrackToObject, bestObjectToTrack) > 0) {
        }

        // Construct return value
        int[][] result = new int[2][];
        result[0] = assignedTrackToObject;
        // Incredibly painful way to create an array of indices of objects which are unassigned
        ArrayList<Integer> newObjs = new ArrayList<>();
        for (int i = 0; i < assignedObjectToTrack.length; i++) {
            if (assignedObjectToTrack[i] == OBJ_NEW_TRACK)
                newObjs.add(i);
        }
        result[1] = new int[newObjs.size()];
        for (int i = 0; i < result[1].length; i++)
            result[1][i] = newObjs.get(i);
//        if (newObjs.size() > 0) {
//            System.out.println("objects which require new tracks: " + Util.join(result[1], ", "));
//            System.out.println("objectDistances:");
//            StringBuilder buf = new StringBuilder();
//            for (int row = 0; row < numObjects; row++) {
//                for (int col = 0; col < numObjects; col++) {
//                    buf.append(Util.padLeft(Math.round(objectDistances[row][col]), 8));
//                }
//                buf.append("\n");
//            }
//            System.out.println(buf);
//        }

        return result;
    }

    /** Returns an array of distances between detected objects and track predictions.
     *
     * @param tracksArr array of existing tracks.
     * @param detectedObjsArr Array of objects detected in this frame.
     * @param nullObjectValue value to store in matrix if detected or tracking object is null.
     * @param square if true, distances are squared.
     * @return matrix M[di][ti] of distances squared, where di is the detected object index, and ti is the track index.
     */
    private double[][] buildDistanceMatrix(TrackWithEllipse[] tracksArr, MotionDetector.DetectedObject[] detectedObjsArr, double nullObjectValue, boolean square) {
        // Discrepancy sq matrix
        final int ndi = detectedObjsArr.length;
        final int nti = tracksArr.length;
        double[][] discrepancySq = new double[ndi][nti];

        // Build matrix
        for (int di = 0; di < ndi; di++) {
            MotionDetector.DetectedObject detected = detectedObjsArr[di];
            for(int ti = 0; ti < nti; ti++) {
                KalmanTrack tracked = tracksArr[ti];
                double discrepancy = detected == null || tracked == null ? nullObjectValue : tracked.assess(detected.centroid);
                discrepancySq[di][ti] = (square ? discrepancy * discrepancy : discrepancy);
            }
        }
        return discrepancySq;
    }

    /** Returns an array of the weighted times that tracks were last detected. */
    private double[] buildTimesVector(TrackWithEllipse[] tracksArr, double ageWeighting, int frameNumber) {
        double[] result = new double[tracksArr.length];
        for(int ti = 0; ti < tracksArr.length; ti++) {
            KalmanTrack track = tracksArr[ti];
            result[ti] = ageWeighting * (frameNumber - track.getLastDetectedAt());
        }
        return result;
    }

    private int simpleAssignments(double[][] distances, double[] trackAges, double maxJump, int[] assignedTrackToObject, int[] assignedObjectToTrack, int[] bestTrackToObject, int[] bestObjectToTrack) {
        int successfulAssignments = 0;

        // Find the closest object to each track
        for (int i = 0; i < bestTrackToObject.length; i++)
            bestTrackToObject[i] = bestObjectForTrack(distances, i);
        // Find the closest track to each object
        for (int i = 0; i < bestObjectToTrack.length; i++)
            bestObjectToTrack[i] = bestTrackForObject(distances, trackAges, i, maxJump);
        for (int tr = 0; tr < bestTrackToObject.length; tr++) {
            final int obj = bestTrackToObject[tr];
            // if it's the best AND it's not too big a jump
            if (obj >= 0 && bestObjectToTrack[obj] == tr && distances[obj][tr] < maxJump) {
                assignedTrackToObject[tr] = obj;
                assignedObjectToTrack[obj] = tr;
                // Adjust distances so that we don't try to re-assign these tracks or objects
                fillRow(distances, obj, Double.MAX_VALUE);
                fillCol(distances, tr, Double.MAX_VALUE);

                successfulAssignments++;
            }
        }

//        System.out.println("Tracks to objects: " + Util.join(assignedTrackToObject, ", "));
//        System.out.println("Objects to tracks: " + Util.join(assignedObjectToTrack, ", "));
        return successfulAssignments;
    }

    private void fillCol(double[][] matrix, int col, double value) {
        for (int row = 0; row < matrix.length; row++)
            matrix[row][col] = value;
    }

    private void fillRow(double[][] matrix, int row, double value) {
        for (int col = 0; col < matrix[row].length; col++)
            matrix[row][col] = value;
    }

    private int bestObjectForTrack(double[][] distances, int col) {
        int minRow = -1;
        double minValue = Double.MAX_VALUE;
        for (int row = 0; row < distances.length; row++) {
            final double d = distances[row][col];
            if (d < minValue) {
                minValue = d;
                minRow = row;
            }
        }
        return minRow;
    }

    private int bestTrackForObject(double[][] distances, double[] trackAges, int row, double maxJump) {
        int minCol = -1;
        double minValue = Double.MAX_VALUE;
        for (int ti = 0; ti < distances[row].length; ti++) {
            final double d = distances[row][ti] + trackAges[ti];
            if (distances[row][ti] < maxJump && d < minValue) {
                minValue = d;
                minCol = ti;
            }
        }
        return minCol;
    }

    // ================================================================================
    // Testing

    public static void main(String[] args) {
        // load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        MultiTracker mt = new MultiTracker(new KalmanTrack.Cfg("fast"), 0, true);
        // Invent some objects
        List<MotionDetector.DetectedObject> objects = new ArrayList<>();
        objects.add(new MotionDetector.DetectedObject(new Point(0, 0)));
        objects.add(new MotionDetector.DetectedObject(new Point(2, 1)));
        objects.add(new MotionDetector.DetectedObject(new Point(100, 100)));

        List<TrackWithEllipse> tracks = new ArrayList<>();
        Params params = new Params();
        Size size = new Size(1000, 1000);
        tracks = mt.trackMulti(objects, tracks, params, true, size, 100);
        System.out.println("tracks = " + tracks);

        objects = new ArrayList<>();
        objects.add(new MotionDetector.DetectedObject(new Point(0, 0)));
        objects.add(new MotionDetector.DetectedObject(new Point(98, 98)));
        tracks = mt.trackMulti(objects, tracks, params, true, size, 110);
        System.out.println("tracks = " + tracks);

    }
}
