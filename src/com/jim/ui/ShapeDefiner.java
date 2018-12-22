package com.jim.ui;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.opencv.core.Point;

/** Abstract base class to assist with interactively defining a graphical shape. */
abstract class ShapeDefiner {
    private ImageView view;
    private Cursor originalCursor;

    void startDefining(ImageView view) {
        this.view = view;

        // Save old cursor and setup a new one
        originalCursor = view.getScene().getCursor();
        view.getScene().setCursor(Cursor.CROSSHAIR);
    }

    Point getMousePoint(MouseEvent event) {
        // Calculate how the image has been scaled to fit in the window.
        return scalePointToImage(new Point(event.getX(), event.getY()));
    }

    Point scalePointToImage(Point p) {
        // Recalculate each time (rather than saving the values) as
        // the window could be resized while measuring
        Bounds bounds = view.getBoundsInParent();
        double scaleX = view.getImage().getWidth() / bounds.getWidth();
        double scaleY = view.getImage().getHeight() / bounds.getHeight();
        return new Point(p.x * scaleX, p.y * scaleY);
    }

    void stopDefining() {
        view.setOnMousePressed(null);
        view.setOnMouseDragged(null);
        view.setOnMouseReleased(null);
        view.setOnMouseMoved(null);
        view.getScene().setCursor(originalCursor);
    }
}
