package com.jim.tracking;

import com.google.gson.Gson;
import com.jim.Params;
import com.jim.util.Util;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/** Defines a spatial region which can be used to define a subset of the area of a video to be processed. */
@SuppressWarnings("WeakerAccess")
public class Region {
    private final boolean includeRegion;
    // List of lists of points. Each inner list is a polygon
    private final List<List<Point>> points;
    // Points in a format usable by opencv polygon drawing methods
    private transient List<MatOfPoint> pts;
    // Points in a format suitable for point in polygon tests
    private transient List<MatOfPoint2f> pts2f;

    public Region(boolean includeRegion, List<List<Point>> points) {
        this.includeRegion = includeRegion;
        this.points = points;
    }

    public Region(boolean includeRegion, Rect rect) {
        this.includeRegion = includeRegion;
        List<Point> list = Arrays.asList(
                new Point(rect.x, rect.y),
                new Point(rect.x, rect.y + rect.height),
                new Point(rect.x + rect.width, rect.y + rect.height),
                new Point(rect.x + rect.width, rect.y)
        );
        this.points = new ArrayList<>();
        this.points.add(list);
    }

    public Region(Region other) {
        this.includeRegion = other.includeRegion;
        this.points = other.points;
    }

    public Region(Reader json) throws IOException {
        Gson gson = new Gson();
        Region other = gson.fromJson(json, getClass());
        if (other == null)
            throw new IOException("Unable to read JSON from mask file");
        this.includeRegion = other.includeRegion;
        this.points = other.points;
    }

    /** True if the region of interest is inside this region, false if it is outside. */
    public boolean isIncludeRegion() {
        return includeRegion;
    }

    /** Transforms this Region according to the transforms applied to the video.
     *
     * @param srcParams Parameters which describe the transforms to be applied.
     * @param inSize Size of video this Region is being applied to.
     * @param reverse If {@code true}, transforms are applied in reverse.
     * @return A possibly new Region which is {@code thi}, transformed according to the parameters.
     */
    public Region transformForParams(Params.SrcVideoParams srcParams, Size inSize, boolean reverse) {
        Util.OrthoRotation angle = srcParams.angle;
        Region region = this;

        if (reverse) {
            angle = angle.negative();
            // Resize
            if (srcParams.resizeWidth != 0) {
                Size rotatedSize = angle.rotateSize(inSize);
                double scale = srcParams.resizeWidth / rotatedSize.width;
                region = region.scale(scale);
            }
            // Perhaps rotate
            Size rotatedSize = angle.rotateSize(inSize);
            region = angle.rotate(region, rotatedSize);

        } else {
            // Perhaps rotate
            region = angle.rotate(region, inSize);
            // Resize
            if (srcParams.resizeWidth != 0) {
                Size rotatedSize = angle.rotateSize(inSize);
                double scale = rotatedSize.width / srcParams.resizeWidth;
                region = region.scale(scale);
            }
        }
        return region;
    }



    /** Returns a new Region which is {@code this} with all points scaled relative to (0, 0).
     *
     * @param scale Factor to scale by.
     * @return A new Region
     */
    public Region scale(double scale) {
        ArrayList<List<Point>> p = transformPoints(point -> new Point(point.x * scale, point.y * scale));
        return new Region(includeRegion, p);
    }

    /** Returns a new Region which is {@code this} with all points translated by ({@code x}, {@code y}).
     *
     * @param dx Delta x.
     * @param dy Delta y.
     * @return A new Region
     */
    public Region translate(double dx, double dy) {
        return new Region(includeRegion, transformPoints(point -> new Point(point.x + dx, point.y + dy)));
    }

    /** Returns a new Region which is this with all points rotated around {@code (0, 0)}.
     *
     * @param angle Rotation angle in degrees.
     * @return A new Region
     */
    public Region rotate(double angle) {
        return rotate(angle, new Point());
    }

    /** Returns a new Region which is this with all points rotated around {@code origin}.
     *
     * @param angle Rotation angle in degrees.
     * @param origin Point to rotate about.
     * @return A new Region
     */
    public Region rotate(double angle, Point origin) {
        double a = Math.toRadians(angle);
        double cosA = cos(a);
        double sinA = sin(a);
        ArrayList<List<Point>> p = transformPoints(point ->
                new Point(((point.x - origin.x) * cosA) - ((point.y - origin.y) * sinA) + origin.x,
                        ((point.x - origin.x) * sinA) + ((point.y - origin.y) * cosA) + origin.y));
        return new Region(includeRegion, p);
    }



    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this, getClass());
    }

    /** Draws this region to the specified matrix.
     * @param image Image to draw to.
     * @param outlineColour If not null, the shape will be outlined in this colour.
     * @param fillColour If not null, the shape will be filled in this colour.
     * */
    public void draw(Mat image, Scalar outlineColour, Scalar fillColour) {
        // Convert list of lists to list of MatOfPoints
        List<MatOfPoint> pts = getMatOfPoints();

        if (outlineColour != null)
            Imgproc.polylines(image, pts, true, outlineColour, 4);
        if (fillColour != null)
            Imgproc.fillPoly(image, pts, fillColour);
    }

    /** Clears all pixels in {@code frame} and either inside
     * (if {@link #isIncludeRegion() includeRegion} is {@code false})
     * or outside (if {@link #isIncludeRegion() includeRegion} is {@code true})
     * of this Region.
     *
     * @param frame Frame to be masked.
     * @param maskColour Colour to be used for parts of the image outside this Region.
     * @return New frame which is a copy of {@code frame} masked by this Region.
     */
    public Mat mask(Mat frame, Scalar maskColour) {
        Mat newFrame = new Mat(frame.size(), frame.type(), maskColour);
        Mat mask = Mat.zeros(frame.size(), CvType.CV_8U);
        draw(mask, null, Util.WHITE);
        if (!includeRegion)
            Core.bitwise_not(mask, mask);
        frame.copyTo(newFrame, mask);
        return newFrame;
    }

    /** Returns the minimum bounding rectangle of this region. */
    public Rect bounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (List<Point> pointList : points) {
            for (Point point : pointList) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
            }
        }
        return new Rect(new Point(minX, minY), new Point(maxX, maxY));
    }

    /** Returns the distance of point from this region. The distance will be 0 if the point lies inside or on the
     * border.
     * The value of the field {@link #isIncludeRegion() includeRegion} is ignored.  */
    public double pointInside(Point point) {
        double dist = 0;
        for (MatOfPoint2f points : getMatOfPoints2f()) {
            double d = Imgproc.pointPolygonTest(points, point, true);
            if (d > dist)
                dist = d;
        }
        return dist;
    }

    @Override
    public String toString() {
        return "Region[inclusive=" + includeRegion + ",bounds=" + bounds() + "]";
    }

// =====================================================================
    // Private methods

    private List<MatOfPoint> getMatOfPoints() {
        if (pts == null) {
            pts = new ArrayList<>();
            for (List<Point> point : points) {
                pts.add(new MatOfPoint(point.toArray(new Point[0])));
            }
        }
        return pts;
    }

    private List<MatOfPoint2f> getMatOfPoints2f() {
        if (pts2f == null) {
            pts2f = new ArrayList<>();
            for (MatOfPoint pt : getMatOfPoints()) {
                MatOfPoint2f dst = new MatOfPoint2f();
                pt.convertTo(dst, CvType.CV_32FC2);
                pts2f.add(dst);
            }
        }
        return pts2f;
    }

    private ArrayList<List<Point>> transformPoints(UnaryOperator<Point> op) {
        ArrayList<List<Point>> p = new ArrayList<>();
        for (List<Point> pointList : points) {
            List<Point> np = new ArrayList<>();
            p.add(np);
            for (Point point : pointList) {
                np.add(op.apply(point));
            }
        }
        return p;
    }

    // ==================================================================
    // Testing

    public static void main(String[] args) throws IOException {
        ArrayList<Point> p1 = new ArrayList<>();
        p1.add(new Point(0, 0));
        p1.add(new Point(0, 1));
        p1.add(new Point(1, 1));
        p1.add(new Point(1, 0));
        ArrayList<Point> p2 = new ArrayList<>();
        p2.add(new Point(100, 0));
        p2.add(new Point(0, 100));
        p2.add(new Point(100, 100));
        p2.add(new Point(100, 0));
        ArrayList<List<Point>> listList = new ArrayList<>();
        listList.add(p1);
        listList.add(p2);
        Region r1 = new Region(true, listList);
        System.out.println("r1.toJson() = " + r1.toJson());
        Region r2 = new Region(new StringReader(r1.toJson()));
        System.out.println("r2.toJson() = " + r2.toJson());
        if (!r1.toJson().equals(r2.toJson()))
            System.out.println("Json representations differ!");
        System.out.println("r1 = " + r1);

        System.out.println("r1.scale(2) = " + r1.scale(2));
        System.out.println("r1.scale(.5) = " + r1.scale(.5));
        System.out.println("r1.rotate(0, new Point()) = " + r1.rotate(0, new Point()));
        System.out.println("r1.rotate(10, new Point()) = " + r1.rotate(10, new Point()));
        System.out.println("r1.rotate(90, new Point()) = " + r1.rotate(90, new Point()).toJson());


        ArrayList<Point> p3 = new ArrayList<>();
        p3.add(new Point(0, 0));
        p3.add(new Point(0, 2));
        p3.add(new Point(1, 2));
        p3.add(new Point(1, 0));
        ArrayList<List<Point>> listList3 = new ArrayList<>();
        listList3.add(p3);
        Region r3 = new Region(true, listList3);
        System.out.println("r3 = " + r3.toJson());
        System.out.println("r3 = " + r3.rotate(-90, new Point(2, 1)).toJson());

        System.out.println("r3.translate(-1, 0) = " + r3.translate(-1, 0));
        System.out.println("r3.translate(0, 1) = " + r3.translate(0, 1));

    }
}
