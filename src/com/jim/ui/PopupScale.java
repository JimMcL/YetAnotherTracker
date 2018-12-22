package com.jim.ui;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.opencv.core.Size;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.function.Consumer;

/** Popup dialog which allows the user to define a scale based on a
 * screen distance (in pixels) and a corresponding known real-world distance. */
@SuppressWarnings("WeakerAccess")
public class PopupScale extends Popup {

    private static final DecimalFormat TWO_D_PLACES = new DecimalFormat("##.00");
    private static final DecimalFormat FIVE_D_PLACES = new DecimalFormat("##.00000");

    /** Represents a known scale in pixels/&lt;units>. */
    @SuppressWarnings("WeakerAccess")
    public static class Scale {
        /** Scale - may be {@code Double.NaN}. */
        public double scale;
        /** Real world units. */
        public String units;

        public Scale(double scale, String units) {
            this.scale = scale;
            this.units = units;
        }

        @Override
        public String toString() {
            return isDefined() ? FIVE_D_PLACES.format(scale) + " pixels/" + units : "<no scale>";
        }

        public boolean isDefined() {
            return !Double.isNaN(scale);
        }
    }

    private Scale scale;
    @FXML private TextField knownDistanceFld;
    @FXML private TextField unitsFld;
    @FXML private Label scaleLbl;
    @FXML private Label measuredDistanceLbl;
    @FXML private Button copyBtn;
    @FXML public Label imageSizeLbl;


    // Measured distance in pixels
    private double measuredDistance = Double.NaN;

    // ========================================================================
    // Methods

    /** Creates and displays a new PopupScale window.
     * @param scale Initial scale (may be NaN).
     * @param units Initial units.
     * @param owner The window will always be on top of the owner.
     * @param okListener Called when the OK button is pressed.
     * */
    public static PopupScale show(double scale, String units, Stage owner, Consumer<Popup> okListener, Consumer<Popup> cancelListener)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(PopupScale.class.getResource("fxml/ScalePopup.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Set Scale");
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        PopupScale controller = loader.getController();
        controller.setScale(new Scale(scale, units));
        controller.updateDisplay();
        controller.setOkListener(okListener);
        controller.setCancelListener(cancelListener);
        scene.setOnKeyPressed(controller::onKeyPressed);

        return controller;
    }

    /** FXML initializer */
    public void initialize() {
        knownDistanceFld.textProperty().addListener((obs, oldValue, newValue) -> updateDisplay());
        unitsFld.textProperty().addListener((obs, oldValue, newValue) -> updateDisplay());
        // Seems a bit complicated for getting an image on a button
        copyBtn.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("copy16.png"))));
    }

    public void setScale(Scale scale) {
        this.scale = scale;
        unitsFld.setText(scale.units);
    }

    public Scale getScale() {
        return scale;
    }

    public void updateHelpMsg(Size size) {
        imageSizeLbl.setText(Math.round(size.width) + "x" + Math.round(size.height) + " pixels");
    }

    public void setMeasuredDistance(double pixels) {
        measuredDistance = pixels;
        updateDisplay();
    }

    // ==========================================================================
    // UI Actions

    @SuppressWarnings("unused")
    public void copyScaleToClipboard(ActionEvent event) {
        doCopy();
    }

    protected void doCopy() {
        final ClipboardContent content = new ClipboardContent();
        BigDecimal bd = new BigDecimal(scale.scale).round(new MathContext(6));
        content.putString("--view-scale " + bd.toPlainString() + " --csv-units " + scale.units);
        Clipboard.getSystemClipboard().setContent(content);
    }


    // ==========================================================================
    // Private methods

    private void updateDisplay() {
        final PseudoClass errorClass = PseudoClass.getPseudoClass("error");
        double knownDistance = Double.NaN;
        try {
            knownDistance = Double.parseDouble(knownDistanceFld.getText());
            knownDistanceFld.pseudoClassStateChanged(errorClass, false);
        } catch (NumberFormatException e) {
//            e.printStackTrace();
            // Subtly indicate that known distance is not valid
            knownDistanceFld.pseudoClassStateChanged(errorClass, true);
        }
        try {
            scale.units = unitsFld.getText();
            double newScale = measuredDistance / knownDistance;
            if (!Double.isNaN(newScale))
                scale.scale = newScale;
        } catch (NumberFormatException e) {
            // Ignore
            e.printStackTrace();
        }
        // Update the scale
        scaleLbl.setText(this.scale.toString());

        // Update measured distance display since it includes the scaled value
        String dist = "-";
        if (!Double.isNaN(measuredDistance)) {
            dist = TWO_D_PLACES.format(measuredDistance) + " pixels";
            if (scale.isDefined()) {
                BigDecimal bd = new BigDecimal(measuredDistance / scale.scale);
                bd = bd.round(new MathContext(3));
                dist += " (" + bd + " " + scale.units + ")";
            }
        }
        measuredDistanceLbl.setText(dist);
    }
}
