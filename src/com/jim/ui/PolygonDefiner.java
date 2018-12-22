package com.jim.ui;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Accepts mouse movements for interactively defining a polygon.
 * The actual drawing is performed by an instance of
 * the interface {@link Handler}. */
public class PolygonDefiner extends ShapeDefiner {


    private static final double SNAP_DIST = 10;

    /** Interface to draw a measurement indicator. */
    public interface Handler {
        /** Called with the the points that make up the polygon.
         * Units are pixels on the source image, i.e. if the
         * image has size 200x100 and a point is at the bottom
         * right corner, its coordinates will be 200, 100
         * regardless of how the view is scaled in the window.
         *
         * @param points The points that make up the polygon.
         */
        void drawPoly(List<Point> points);
    }

    private Handler drawer;
    private Consumer<PolygonDefiner> onFinished;
    private List<Point> points = new ArrayList<>();

    public PolygonDefiner(Handler drawer, Consumer<PolygonDefiner> onFinished) {
        this.drawer = drawer;
        this.onFinished = onFinished;
    }

    public List<Point> getPoints() {
        return points;
    }

    public PolygonDefiner startDrawing(ImageView view) {
        super.startDefining(view);

        view.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.SECONDARY) {
                if (points.size() == 0) {
                    addMousePoint(event);
                    drawer.drawPoly(points);
                }
                event.consume();
            }
        });
        view.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                // Remove the last point
                points.remove(points.size() - 1);
                onFinished.accept(this);
            } else {
                addMousePoint(event);
                drawer.drawPoly(points);
            }
            event.consume();
        });
        EventHandler<MouseEvent> moveHandler = event -> {
            // Replace last point with this one
            int np = points.size();
            if (np > 1) {
                points.set(np - 1, getMousePoint(event));
                drawer.drawPoly(points);
            }
            event.consume();
        };
        view.setOnMouseDragged(moveHandler);
        view.setOnMouseMoved(moveHandler);

        return this;
    }

    @Override
    Point getMousePoint(MouseEvent event) {
        Node target = (Node) event.getTarget();
        Bounds bounds = target.getBoundsInLocal();
        Point p = scalePointToImage(snapToEdge(event.getX(), event.getY(), bounds.getWidth(), bounds.getHeight()));
        return p;
    }

    private void addMousePoint(MouseEvent event) {
        points.add(getMousePoint(event));
    }

    private Point snapToEdge(double x, double y, double width, double height) {
        if (x < SNAP_DIST)
            x = 0;
        else if (width - x < SNAP_DIST)
            x = width;
        if (y < SNAP_DIST)
            y = 0;
        else if (height - y < SNAP_DIST)
            y = height;
        return new Point(x, y);
    }

    public void stopDrawing () {
        stopDefining();
    }

}
