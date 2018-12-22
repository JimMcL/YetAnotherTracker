package com.jim.util;

import com.jim.tracking.Region;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.transpose;

/**
 * A set of utility methods for use with openCV.
 *
 * Some are based on https://opencv-java-tutorials.readthedocs.io/en/latest/03-first-javafx-application-with-opencv.html
 */
@SuppressWarnings("WeakerAccess")
public class Util {

    public static Scalar WHITE = RGB(255, 255, 255);
    public static Scalar BLACK = RGB(0, 0, 0);

    /** Converts an openCV Mat image to a Swing Image. */
    static public Image mat2BufferedImage(Mat m) {
        try
        {
            MatOfByte byteMat = new MatOfByte();
            Imgcodecs.imencode(".bmp", m, byteMat);
            return new Image(new ByteArrayInputStream(byteMat.toArray()));
        }
        catch (Exception e) {
            System.err.println("Cannot convert the Mat object: " + e);
            return null;
        }
    }

    /** Converts an openCV MatOfPoint2f to a MatOfPoint. */
    public static MatOfPoint matOfPoint2fToMatOfPoint(MatOfPoint2f m2f) {
        MatOfPoint mp = new MatOfPoint();
        m2f.convertTo(mp, CvType.CV_32S);
        return mp;
    }

    /**
     * Generic method for putting element running on a non-JavaFX thread on the
     * JavaFX thread, to properly update the UI
     *
     * @param property an {@link ObjectProperty}
     * @param value the value to set for the given {@link ObjectProperty}
     */
    public static <T> void setOnFXThread(final ObjectProperty<T> property, final T value) {
        Platform.runLater(() -> property.set(value));
    }

    /** Converts red, green, blue [0, 255] values to a GBR Scalar for use with openCV. */
    public static Scalar RGB(int red, int green, int blue) {
        return new Scalar(blue, green, red);
    }

    /** Converts a string of format "<width>x<height>" to a Size instance.
     * @param s Size as a string
     * @return Size instance
     */
    public static Size parseSize(String s) {
        try {
            String[] parts = s.split("x");
            return new Size (Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (Exception e) {
            throw new RuntimeException("Invalid size '" + s + "': expected '<width>x<height>");
        }
    }

    /** Returns the distance between 2 points. */
    public static double distance(Point p1, Point p2) {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }

    /** Returns {@code aClass} or the first ancestor of {@code aClass}
     * whose name is not the empty string.
     */
    public static Class getFirstNamedAncestor(Class aClass) {
        while(aClass != null && "".equals(aClass.getSimpleName()))
            aClass = aClass.getSuperclass();
        return aClass;
    }

    /** Adjusts {@code Size} to have the specified new width
     * while preserving the current aspect ratio. */
    public static Size setWidthPreservingAR(double newWidth, Size arSize) {
        return new Size(newWidth, newWidth * arSize.height / arSize.width);
    }

    public enum OrthoRotation {
        NONE(0), CW_90(-90), CCW_90(90), CW_180(180);

        private int angle;
        OrthoRotation(int angle) {
            this.angle = angle;
        }

        public static int[] validAngles() {
            final OrthoRotation[] values = OrthoRotation.values();
            int[] r = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                r[i] = values[i].angle;
            }
            return r;
        }

        public OrthoRotation negative() {
            switch (this) {
                case NONE:
                    return NONE;
                case CW_90:
                    return CCW_90;
                case CCW_90:
                    return CW_90;
                case CW_180:
                    return CW_180;
            }
            return NONE;
        }

        public static OrthoRotation fromAngle(int angle) {
            for (OrthoRotation rotation : OrthoRotation.values()) {
                if (rotation.angle == angle)
                    return rotation;
            }
            throw new RuntimeException("Invalid rotation (" + angle + "), must be one of (" + Util.join(validAngles(), ", ") + ")");
        }

        public Size rotateSize(Size size) {
            switch(this) {
                case NONE:
                case CW_180:
                    break;
                case CW_90:
                case CCW_90:
                    // Transpose height and width
                    //noinspection SuspiciousNameCombination
                    size = new Size(size.height, size.width);
                    break;
            }
            return size;
        }

        /** Rotates a matrix by 90, 180 or -90 degrees. */
        public void rotate(Mat m){
            if (this == OrthoRotation.CW_90){
                transpose(m, m);
                flip(m, m,1); //transpose+flip(1)=CW
            } else if (this == OrthoRotation.CCW_90) {
                transpose(m, m);
                flip(m, m, 0); //transpose+flip(0)=CCW
            } else if (this == OrthoRotation.CW_180){
                flip(m, m,-1);    //flip(-1)=180
            }
        }

        /** Rotate the specified {@code Region} as though it is embedded within a matrix of size {@code origSize},
         * i.e. the new top-left corner is the origin after rotation. */
        public Region rotate(Region region, Size origSize) {
            switch (this) {
                case NONE:
                    return region;
                case CW_90:
                    return region.translate(0, -origSize.height).rotate(90);
                case CCW_90:
                    return region.translate(-origSize.width, 0).rotate(-90);
                case CW_180:
                    return region.translate(-origSize.width, -origSize.height).rotate(180);
            }
            return region;
        }
    }

    /** Given a preferred window width, constrains it so that it
     * fits onto the primary screen with the specified aspect ratio. */
    public static double constrainedWidthToScreen(double width, double aspectRatio) {
        Rectangle2D rect = Screen.getPrimary().getVisualBounds();
        double maxWidth = rect.getWidth() - 100;
        double maxHeight = rect.getHeight() - 100;
        double widthForMaxHeight = maxHeight / aspectRatio;
        return Math.min(Math.min(width, maxWidth), widthForMaxHeight);
    }

    /** Replaces the extension part of a file name with a new extension.
     *
     * @param file Original file name.
     * @param newExtension New extension including dot, e.g. ".csv".
     * @return {@code file} with the extension replaced by {@code extension}.
     */
    public static String replaceExtension(String file, String newExtension) {
        // Crappy way to replace an extension
        final String name = new File(file).getName();
        int idx = name.lastIndexOf('.');
        String basename = file;
        if (idx != -1) {
            int extLen = name.length() - idx;
            basename = file.substring(0, file.length() - extLen);
        }

        return basename + newExtension;
    }


    // =====================================================================================

    public static String join(Object[] l, String sep) {
        StringBuilder buf = new StringBuilder();
        String ts = "";
        for (Object o : l) {
            buf.append(ts).append(o.toString());
            ts = sep;
        }
        return buf.toString();
    }

    public static <T> String join(List<T> l, String sep) {
        return Util.join(l.toArray(), sep);
    }

    public static String join(int[] a, String sep) {
        return join(Arrays.stream(a).boxed().toArray(Integer[]::new), sep);
    }

    public static String join(double[] a, String sep) {
        return join(Arrays.stream(a).boxed().toArray(Double[]::new), sep);
    }

    public static String padRight(Object s, int n) {
        return String.format("%1$-" + n + "s", s == null ? null : s.toString());
    }

    public static String padLeft(Object s, int n) {
        return String.format("%1$" + n + "s", s == null ? null : s.toString());
    }

    public static String formatPoint(Point p) {
        return p == null ? null : Math.round(p.x) + "x" + Math.round(p.y);
    }

    public static String basename(String file) {
        return new File(file).getName();
    }

    public static Scalar idToRGB(long id) {
        final int r = (int) ((id & 1) * 240);
        final int g = (int) (((id >> 1) & 1) * 240);
        final int b = (int) (((id >> 2) & 1) * 240);
        return RGB(r, g, b);
    }

//    public static boolean intersect(Rect r1, Rect r2) {
//
//    }
}
