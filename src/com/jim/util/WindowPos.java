package com.jim.util;

import javafx.beans.Observable;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.opencv.core.Point;

import java.util.prefs.Preferences;

/** Utility to store a JavaFX window's origin in the user's preferences and
 * restore it when a new instance is created.
 *
 * It is used something like this:
 * <pre>{@code stage.show();
 * WindowPos.rememberWindowOrigin(stage, Controller.class, windowTitle);}
 * </pre>
 */
public class WindowPos {
    // Preferences keys
    private static final String X = "x";
    private static final String Y = "y";
    private static final String MAXIMIZED = "maximized";
    // Name of preferences node
    private final String prefsNodePath;
    private final Stage stage;

    /** Remember the specified window's origin.
     *
     * @param stage The window. It's origin will be set to the previous origin if it is known.
     * @param aClass Class used to identify the window.
     * @param title Optional title (may be null) used to identify the window.
     */
    public static void rememberWindowOrigin(Stage stage, Class aClass, String title) {
        WindowPos wp = new WindowPos(stage, aClass, title);

        Point origin = wp.getOriginPreference();
        if (origin != null) {
            // Check that the window will be mainly visible at this position
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            int cx = (int) (origin.x + stage.getWidth() / 2);
            int cy = (int) (origin.y + stage.getHeight() / 2);
            // if the centre of the window is one the screen, consider it adequately visible
            if (cx > 0 && cx < screen.getWidth() &&
                    cy > 0 && cy < screen.getHeight()) {
                stage.setX(origin.x);
                stage.setY(origin.y);
            }
        }

        if (wp.getMaximizedPreference()) {
            stage.setMaximized(true);
        }
    }

    // ==========================================================================
    // Private methods

    private WindowPos(Stage stage, Class controller, String title) {
        // Create (hopefully) unique name for the window preference
        StringBuilder buf = new StringBuilder(controller.getCanonicalName().replaceAll("\\.", "/"));
        if (title != null && title.length() > 0)
            buf.append("/").append(title);
        prefsNodePath = buf.toString();

        this.stage = stage;

        // Watch for changes in location, and update origin preference
        stage.xProperty().addListener(this::updateOriginPref);
        stage.yProperty().addListener(this::updateOriginPref);
        stage.maximizedProperty().addListener(this::updateMaximized);
    }

    private Preferences getPreferences() {
        return Preferences.userRoot().node(prefsNodePath);
    }

    private Point getOriginPreference() {
        Preferences pref = getPreferences();
        int x = pref.getInt(X, -1);
        int y = pref.getInt(Y, -1);
        if (x >= 0 && y >= 0)
            return new Point(x, y);
        return null;
    }

    @SuppressWarnings("unused")
    private void updateOriginPref(Observable observable) {
        Preferences prefs = getPreferences();
        prefs.putInt(X, (int) stage.getX());
        prefs.putInt(Y, (int) stage.getY());
    }

    private boolean getMaximizedPreference() {
        Preferences pref = getPreferences();
        return pref.getBoolean(MAXIMIZED, false);
    }

    @SuppressWarnings("unused")
    private void updateMaximized(Observable observable) {
        Preferences prefs = getPreferences();
        prefs.putBoolean(MAXIMIZED, stage.isMaximized());
    }
}
