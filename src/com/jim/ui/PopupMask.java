package com.jim.ui;

import com.jim.Params;
import com.jim.tracking.Region;
import com.jim.util.GraphicPolygon;
import com.jim.util.Util;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Controller for defining a polygon to be used as a mask.
 * Coordinates the dialog, the user interactions for defining the polygon,
 * and the visual representation of the polygon.
 */
public class PopupMask extends Popup {

    /** Object responsible for drawing the (possibly incomplete) polygon. */
    private GraphicPolygon graphicPoly;
    /** Object responsible for obtaining user input (mouse events) to allow the user to define the polygon. */
    private PolygonDefiner polygonDefiner;
    private Params params;
    private Size videoSize;
    private ImageView imageView;
    private Region oldMask;
    private List<Point> newMaskPoints;

    // ========================================================================
    // Methods

    /** Creates and displays a new PopupMask window.
     * @param owner The window will always be on top of the owner.
     * @param params Contains the mask.
     * @param imageView The polygon will be drawn on this view.
     * */
    static PopupMask show(Stage owner, Params params, Size videoSize, ImageView imageView, Consumer<Popup> okListener, Consumer<Popup> cancelListener)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(PopupScale.class.getResource("fxml/MaskPopup.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Set Scale");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        PopupMask controller = loader.getController();
        controller.setParams(params);
        controller.setVideoSize(videoSize);
        controller.setImageView(imageView);
        controller.setOkListener(okListener);
        controller.setCancelListener(cancelListener);
        controller.startNewMask();
        scene.setOnKeyPressed(controller::onKeyPressed);

        return controller;
    }

    private void setVideoSize(Size videoSize) {
        this.videoSize = videoSize;
    }

    private void setParams(Params params) {
        this.params = params;
    }

    Region getOldMask() {
        return oldMask;
    }

    Region getNewMask() {
        return regionFromPoints(newMaskPoints);
    }

    private void setMaskPoints(List<Point> points) {
        this.newMaskPoints = points;
    }

    private void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    void annotate(Mat feedbackImage) {
        if (graphicPoly != null)
            graphicPoly.draw(feedbackImage);
    }

    // ==========================================================================
    // UI Actions

    @SuppressWarnings("unused")
    public void copyMaskToClipboard(ActionEvent event) {
    }

    @SuppressWarnings("unused")
    public void onSave(ActionEvent event) {
        saveRegionToFile(new File(Util.replaceExtension(params.srcParams.videoFile, ".json")));
    }

    @Override
    public void doOK(Event event) {
        stopDrawing();
        super.doOK(event);
    }

    @Override
    public void doCancel(Event event) {
        stopDrawing();
        super.doCancel(event);
    }

    // ============================================================================
    // PolygonDefiner actions

    private void drawMask(List<Point> points) {
        graphicPoly.setCloseWithDifferentSymbology(points.size() > 2);
        graphicPoly.setPoints(points);
        setMaskPoints(points);
    }

    private void onMaskFinished(PolygonDefiner polygonDefiner) {
        polygonDefiner.stopDrawing();
        List<Point> points = polygonDefiner.getPoints();
        setMaskPoints(points);
        graphicPoly.setPoints(points);
        graphicPoly.setCloseWithDifferentSymbology(false);
    }

    // ============================================================================
    // Private methods

    private void startNewMask() {
        oldMask = params.trParams.getMask();
        polygonDefiner = new PolygonDefiner(this::drawMask, this::onMaskFinished).startDrawing(imageView);
        graphicPoly = new GraphicPolygon();
        graphicPoly.setColour(Util.RGB(128, 200, 255));
        graphicPoly.setWidth(4);
        graphicPoly.setCloseWithDifferentSymbology(false);
    }

    private void stopDrawing() {
        polygonDefiner.stopDrawing();
        polygonDefiner = null;
        graphicPoly = null;
    }

    private Region regionFromPoints(List<Point> points) {
        if (points == null)
            return null;
        List<Point> lp = new ArrayList<>(points);
        List<List<Point>> llp = new ArrayList<>();
        llp.add(lp);
        return new Region(true, llp).transformForParams(params.srcParams, videoSize, true);
    }

    private void saveRegionToFile(File file) {
        if (newMaskPoints != null) {
            Region mask = regionFromPoints(newMaskPoints);
            System.out.println("Writing mask to " + file);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(mask.toJson());
            } catch (Exception e) {
                ExceptionAlert.show("Save error", e);
                e.printStackTrace(System.err);
            }
        }
    }
}
