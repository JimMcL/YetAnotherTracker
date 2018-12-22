package com.jim.tracking;

import com.jim.util.Util;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.video.KalmanFilter;

import static org.opencv.core.Core.setIdentity;
import static org.opencv.core.CvType.CV_32F;

/**
 * Kalman filter applied to a single object. Basic usage is create new object,
 * then for each frame, call {@link #assess(Point) assess} then either {@link #apply(Point) apply}
 * if the object position is known, or one of {@link #stopped() stopped} or
 * {@link #continueAsPredicted() continueAsPredicted} if it's not.
 */
public class KalmanTrack {
    /** The actual kalman filter. */
    private final KalmanFilter filter;
    private final Mat measurementMatrix;
    private final long trackId;
    private final Point initialPosition;
    // True if assess has been called since the last position was applied
    private boolean assessCalled;
    // Number of points which have been applied
    private int pointCount = 0;
    private Point lastAppliedPoint;
    private Point lastPredictedPoint;
    private Point currentPosition;
    private boolean positionChanged = false;
    private int lastDetectedAt = 0;
    private int changeCount = 0;

    /** Defines a set of configurable parameters for the Kalman filter. */
    public static class Cfg {
        private final String s;
        double processNoiseCov = 1e-4;
        double measurementNoiseCov = 1e-1;
        double errorCovPost = .1;

        private void init(double processNoiseCov, double measurementNoiseCov, double errorCovPost) {
            this.processNoiseCov = processNoiseCov;
            this.measurementNoiseCov = measurementNoiseCov;
            this.errorCovPost = errorCovPost;
        }

        public Cfg(String s) {
            if (s == null)
                s = "normal";

            this.s = s;

            String[] names = new String[]{"veryfast", "fast", "normal", "slow", "veryslow"};
            switch (s) {
                case "veryslow":
                    init(1e-10, 1e-2, 1e-2);
                    break;
                case "slow":
                    init(1e-6, 1e-2, 1e-2);
                    break;
                case "normal":
                    init(1e-4, 1e-1, 1e-1);
                    break;
                case "fast":
                    init(1e-3, 1e-1, 1e-1);
                    break;
                case "veryfast":
                    init(1e-1, 1, 1);
                    break;
                default:
                    try {
                        String[] parts = s.split(",");
                        init(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid Kalman filter parameters '" + s +
                                "':\n\texpected '<process noise covariance>,<measurement noise covariance>,<error covariance post>\n\t" +
                                "or one of " + Util.join(names, ", "));
                    }
                    break;
            }
        }

        @Override
        public String toString() {
            return s;
        }
    }

    // =======================================================================================

    /** Constructor.
     *
     * @param trackId Id of this track.
     * @param cfg Parameters controlling smoothness of filter.
     * @param initialPosition Starting object position.
     */
    public KalmanTrack(long trackId, Cfg cfg, Point initialPosition) {
        this.trackId = trackId;
        this.initialPosition = initialPosition;

        // Based on http://www.morethantechnical.com/2011/06/17/simple-kalman-filter-for-tracking-using-opencv-2-2-w-code/
        // 4 dynamic parameters, x, y position and x, y velocity
        // 2 measurement parameters, x, y position
        // 0 control parameters (?)
        filter = new org.opencv.video.KalmanFilter(4, 2, 0, CV_32F);

        // Transition matrix
        Mat tm = new Mat(4,4, CV_32F, new Scalar(0));
        tm.put(0,0, new float[] {1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1});
        filter.set_transitionMatrix(tm);

        // Measurement matrix
//        measurementMatrix = new Mat(2, 1, CV_32F, new Scalar(0));
        measurementMatrix = new Mat(2, 1, CV_32F, new Scalar(0));

        // Initialise state matrix
        Mat statePre = new Mat(4,1, CV_32F);
        statePre.put(0, 0, initialPosition.x);
        statePre.put(1, 0, initialPosition.y);
        statePre.put(2, 0, 0);
        statePre.put(3, 0, 0);
        filter.set_statePre(statePre);
        Mat statePost = new Mat(4,1, CV_32F);
        statePost.put(0, 0, initialPosition.x);
        statePost.put(1, 0, initialPosition.y);
        statePost.put(2, 0, 0);
        statePost.put(3, 0, 0);
        filter.set_statePost(statePost);

        setIdentity(filter.get_measurementMatrix());
        setIdentity(filter.get_processNoiseCov(), Scalar.all(cfg.processNoiseCov));
        setIdentity(filter.get_measurementNoiseCov(), Scalar.all(cfg.measurementNoiseCov));
        setIdentity(filter.get_errorCovPost(), Scalar.all(cfg.errorCovPost));
    }

    public long getTrackId() {
        return trackId;
    }

    /** Attempts to assess the probability that a position represents this object.
     * Currently it's just the difference between the point and the predicted point. */
    public double assess(Point point) {
        // Only call predict once for every applied point
        if (!assessCalled) {
            assessCalled = true;
            lastPredictedPoint = pointFromMat(filter.predict());
        }
        return Util.distance(lastPredictedPoint, point);
    }

    /** Applies the specified point to this object, returning the corrected position. */
    public Point apply(Point point) {
        lastAppliedPoint = point;
        pointCount++;

        // It's essential to call predict on the filter before correcting it, but avoid calling it twice
        if (!assessCalled)
            assess(point);
        // Need to call assess before the next point is applied
        assessCalled = false;

        // Correct with last available measurement
        measurementMatrix.put(0, 0, point.x);
        measurementMatrix.put(1, 0, point.y);
        Mat corr = filter.correct(measurementMatrix);
        Point newPos = pointFromMat(corr);
        positionChanged = !newPos.equals(currentPosition);
        if (positionChanged)
            changeCount++;
        return currentPosition = newPos;
    }

    /** Re-applies the last applied point. Should be called when the object has stopped. */
    public Point stopped() {
        return apply(lastAppliedPoint);
    }

    /** Assumes that the object is travelling consistently but without detection. Applies the current predicted point. */
    public Point continueAsPredicted() {
        return apply(lastPredictedPoint);
    }

    /** Returns the number of points applied to this object (including {@link #stopped} or {@link #continueAsPredicted} points). */
    public int getAppliedPointCount() {
        return pointCount;
    }

    /** Returns the last point passed to {@link #apply(Point)}. */
    public Point getLastAppliedPoint() {
        return lastAppliedPoint;
    }

    public Point getLastPredictedPoint() {
        return lastPredictedPoint;
    }

    /** Returns the result of the most recent call to {@link #apply(Point)}, {@link #stopped()} or {@link #continueAsPredicted()}.  */
    public Point getCurrentPosition() {
        return currentPosition;
    }

    /** Returns true if the value of currentPosition was altered by the last call to {@link #apply(Point)} etc. */
    public boolean isPositionChanged() {
        return positionChanged;
    }

    /** Returns the number of times the track position has changed. */
    public int getChangeCount() {
        return changeCount;
    }

    /** Returns the distance from the initial position to the current position. */
    public double getDisplacement() {
        return Util.distance(initialPosition, currentPosition);
    }

    /** Returns the number of the frame in which this track was last detected. */
    public int getLastDetectedAt() {
        return lastDetectedAt;
    }

    /** Sets the number of the frame in which this track was last detected. */
    public void setLastDetectedAt(int lastDetectedAt) {
        this.lastDetectedAt = lastDetectedAt;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + Util.formatPoint(getCurrentPosition()) + "," + getAppliedPointCount() + (positionChanged ? ",stopped" : "") + ">";
    }

    // ================================================================================================
    // Private

    private Point pointFromMat(Mat mat) {
        return new Point(mat.get(0, 0)[0], mat.get(1, 0)[0]);
    }


}
