package com.jim.tracking;

import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

/**
 * Created by jim_m on 28-May-17.
 */
public class TrackWithEllipse extends KalmanTrack {
    private final RotatedRect ellipse;

    public TrackWithEllipse(long trackId, Cfg cfg, Point centroid, RotatedRect ellipse) {
        super(trackId, cfg, centroid);
        this.ellipse = ellipse;
    }

    public RotatedRect getEllipse() {
        return ellipse;
    }

    @Override
    public Point apply(Point point) {
        final Point p = super.apply(point);
        if (ellipse != null)
            ellipse.center = p;
        return p;
    }
}
