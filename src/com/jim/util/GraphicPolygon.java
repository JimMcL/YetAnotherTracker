package com.jim.util;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static com.jim.util.Util.RGB;

/** A polygon which can be drawn to an OpenCV Mat. */
public class GraphicPolygon implements GraphicAnnotation {
    // polylines handles multiple sets of points, but we only use the first
    private transient List<MatOfPoint> pts;

    private Scalar colour = RGB(255, 40, 30);
    private Scalar closingColour = RGB(200, 160, 180);
    private boolean closeWithDifferentSymbology;
    private int width = 2;

    public synchronized void setPoints(List<Point> points) {
        pts = new ArrayList<>();
        pts.add(new MatOfPoint(points.toArray(new Point[0])));
    }

    public synchronized List<Point> getPoints() {
        MatOfPoint[] a = pts.toArray(new MatOfPoint[0]);
        if (a.length > 0)
            return a[0].toList();
        return null;
    }

    public void setColour(Scalar colour) {
        this.colour = colour;
    }

    public void setClosingColour(Scalar closingColour) {
        this.closingColour = closingColour;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public synchronized void draw(Mat mat) {
        if (pts != null && pts.size() > 0) {
            // Draw the lines
            Imgproc.polylines(mat, pts, !closeWithDifferentSymbology, colour, width);
            // Optionally close the polygon with a different symbology
            if (closeWithDifferentSymbology) {
                MatOfPoint matOfPoint = pts.get(0);
                Size np = matOfPoint.size();
                Point[] pa = matOfPoint.toArray();
                if (pa.length > 1) {
                    Imgproc.line(mat, pa[0], pa[pa.length - 1], closingColour, width);
                }
            }
        }
    }

    /** If {@code closeWithDifferentSymbology} is {@code true}, the line drawn
     * from the last point to the first is drawn in a different colour than
     * the rest of the lines.
     */
    public void setCloseWithDifferentSymbology(boolean closeWithDifferentSymbology) {
        this.closeWithDifferentSymbology = closeWithDifferentSymbology;
    }
}
