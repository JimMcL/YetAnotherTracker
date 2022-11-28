package com.jim.ui;

import javafx.scene.image.ImageView;
import org.opencv.core.Point;

/** Accepts mouse movements for drawing a measurement line.
 * The actual drawing is performed by an instance of
 * the interface {@link Handler}. */
public class LinearMeasurer extends ShapeDefiner {


    /** Interface to draw a measurement indicator. */
    public interface Handler {
        /** Called with the endpoints of the measurement line.
         * Units are pixels on the source image, i.e. if the
         * image has size 200x100 and a point is at the bottom
         * right corner, its coordinates will be 200, 100
         * regardless of how the view is scaled.
         *  @param p0 Line starting point in pixels.
         * @param p1 Line ending point in pixels.
         */
        void handleMeasurement(Point p0, Point p1);
    }

    private final Handler drawer;
    private Point p0;

    public LinearMeasurer(Handler drawer) {
        this.drawer = drawer;
    }

    public LinearMeasurer startMeasuring(ImageView view) {
        startDefining(view);

        view.setOnMousePressed(event -> {
            p0 = getMousePoint(event);
            event.consume();
        });
        view.setOnMouseDragged(event -> {
            drawer.handleMeasurement(p0, getMousePoint(event));
            event.consume();
        });

        return this;
    }

    public void stopMeasuring() {
        stopDefining();
    }
}
