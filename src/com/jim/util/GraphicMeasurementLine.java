package com.jim.util;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static com.jim.util.Util.RGB;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/** A graphical representation of a line used to measure distances.
 * Visually represented as a line between two points, with short
 * perpendicular cross-pieces at either end. */
public class GraphicMeasurementLine implements GraphicAnnotation {
    private double x0;
    private double y0;
    private double x1;
    private double y1;
    private final Scalar colour = RGB(255, 40, 30);
    private final int width = 2;
    private final double crossPieceLength = 10;

    public synchronized void setPixelCoords(double x0, double y0, double x1, double y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    @Override
    public synchronized void draw(Mat mat) {
        Imgproc.line(mat, new Point(x0, y0), new Point(x1, y1), colour, width);
        // Draw cross-pieces at 90 degrees to the main line
        double angle = Math.atan2(y1 - y0, x1 - x0) + Math.PI / 2;
        double dx = crossPieceLength * cos(angle);
        double dy = crossPieceLength * sin(angle);
        Imgproc.line(mat, new Point(x0 + dx, y0 + dy), new Point(x0 - dx, y0 - dy), colour, 2);
        Imgproc.line(mat, new Point(x1 + dx, y1 + dy), new Point(x1 - dx, y1 - dy), colour, 2);

    }
}
