package com.jim.ui;

import com.jim.Params;
import com.jim.tracking.MotionDetector;
import com.jim.tracking.TrackWithEllipse;
import com.jim.tracking.VideoPlayer;
import com.jim.util.GraphicMeasurementLine;
import com.jim.util.Util;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.IOException;
import java.util.List;

import static com.sun.javafx.application.PlatformImpl.runAndWait;
import static java.lang.StrictMath.sqrt;

public class MainController extends HeadlessController implements MotionDetector.Filter {

    private Stage stage;
    private GraphicMeasurementLine measurementLine;
    private LinearMeasurer measurer;
    private PopupScale popupScale;
    private PopupMask maskPopup;

    private static class FrameTimer {
        private long lastFrameTime;
        private int lastFrameIndex;
        private long playbackFrameInterval;

        void blockTilTime(int frameIndex) {
            // Allow for playback speed
            long now = System.currentTimeMillis();
            final double due = lastFrameTime + playbackFrameInterval * (frameIndex - lastFrameIndex);
            if (due > now)
                try {
                    Thread.sleep((long) (due - now));
                    now = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    // Ignore
                }
            lastFrameTime = now;
            lastFrameIndex = frameIndex;
        }

        void setFPS(double fps, double playbackSpeed) {
            // Convert video frame rate to frame delay in milliseconds
            playbackFrameInterval = (int) (1000 / fps / playbackSpeed);
        }

        void adjustSpeedRelative(double factor) {
            playbackFrameInterval *= factor;
        }
    }

    @FXML private Region container;
    @FXML private Region menuContainer;
    @FXML private Button playButton;
    @FXML private Button slowerBtn;
    @FXML private Button fasterBtn;
    @FXML private ImageView imageView;
    @FXML private AnchorPane paramsForm;
    @FXML private ParamsForm paramsFormController;
//    @FXML
//    public ScrollPane imageScroller;
    @FXML private Label msg;
    @FXML private Slider frameSlider;
    @FXML public Label frameNumLbl;

    // Playback parameters
    private final FrameTimer timer = new FrameTimer();
    private final double zoomLevel = 1;
    private boolean ignoreSliderChanges;
    private VideoPlayer videoPlayer;



    public MainController() {
    }

    /**
     * Magically called by javafx!
     */
    public void initialize() {
        // Make image view resize with container width
        imageView.fitWidthProperty().bind(container.widthProperty());
        // Fit image to height - bottom menu height, but we can't do that until we know the menu height
        menuContainer.heightProperty().addListener((observable, oldValue, newValue) ->
                imageView.fitHeightProperty().bind(container.heightProperty().add(-(Double)newValue)));

        // Set up the slider to change the current frame of the video
//        frameSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
//            if (!ignoreSliderChanges) {
//                if (params.grParams.running)
//                    pausePlayingVideo();
//                videoPlayer.setFrameIndex(newValue.intValue());
//            }
//        });
    }

    @Override
    public void setParams(Params params) {
        super.setParams(params);
        paramsFormController.setParams(params);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // ==========================================================================
    // MotionDetector.Filter actions

    @Override
    public void onCameraOpened(VideoPlayer camera) {
        this.videoPlayer = camera;
        // Handle a still image - useful for calculating scale
        if (videoPlayer.getNumOfFrames() == 1) {
            disablePlaying();
        } else {
            timer.setFPS(camera.getFps(), params.grParams.playbackSpeed);
            adjustSliderToVideo(camera);
            adjustViewToVideo(zoomLevel);
            Platform.runLater(() -> updatePlayButton(params.grParams.running));
        }

        Size size = getScaledVideoSize();
        imageView.maxWidth(size.width);
        imageView.maxHeight(size.height);

        // Do we want to force the user to measure scale?
        if (params.srcParams.manualScale) {
            Platform.runLater(() -> popupScaleDlg(null));
        }
    }

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {
        drawAnnotations(feedbackImage);

        timer.blockTilTime(camera.getFrameIndex());
        displayFrame(camera.getFrameIndex(), feedbackImage);

        // Give the event thread a chance to do some drawing
        try {
            Thread.sleep(0, 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDone(VideoPlayer camera) {
        System.out.println("Finished");
        super.onDone(camera);
    }


    // ==========================================================================
    // GUI actions

    @SuppressWarnings("unused")
    @FXML
    public void togglePlaying(ActionEvent actionEvent) {

        if (!params.grParams.running) {
            // Start video playing
            updatePlayButton(true);
        } else {
            // Pause playing video
            pausePlayingVideo();
        }
    }

    @SuppressWarnings("unused")
    @FXML
    public void playFaster(ActionEvent actionEvent) {
        timer.adjustSpeedRelative(1 / sqrt(2));
    }

    @SuppressWarnings("unused")
    @FXML
    public void playSlower(ActionEvent actionEvent) {
        timer.adjustSpeedRelative(sqrt(2));
    }

    @SuppressWarnings("unused")
    @FXML
    public void zoomIn(ActionEvent actionEvent) {
        adjustViewToVideo(zoomLevel * sqrt(2));
    }

    @SuppressWarnings("unused")
    @FXML
    public void zoomOut(ActionEvent actionEvent) {
        adjustViewToVideo(zoomLevel / sqrt(2));
    }

    @SuppressWarnings("unused")
    @FXML
    public void popupScaleDlg(ActionEvent actionEvent) {
        try {
            if (popupScale == null) {
                popupScale = PopupScale.show(params.srcParams.scale, params.trParams.outputUnits == null ? "m" : params.trParams.outputUnits, stage, this::onScaleChanged, this::onScaleCancelled);
                popupScale.updateHelpMsg(getScaledVideoSize());
                measurer = new LinearMeasurer(this::measureLine).startMeasuring(imageView);
            }
        } catch (IOException e) {
            ExceptionAlert.show(e);
        }
    }

    @SuppressWarnings("unused")
    @FXML public void popupMaskDlg(ActionEvent event) {
        try {
            if (maskPopup == null) {
                maskPopup = PopupMask.show(stage, params, videoPlayer.getSize(), imageView, this::onMaskOk, this::onMaskCancelled);
            }
        } catch (Exception e) {
            ExceptionAlert.show(e);
        }
    }

    // =======================================================================
    // Listener methods

    @SuppressWarnings("unused")
    private void onScaleChanged(Popup popup) {
        PopupScale.Scale scale = (popupScale).getScale();
        params.srcParams.setScale(scale.scale);
        if (params.grParams.verbose)
            System.out.println("Scale set to " + scale);

        clearMeasuringLine();
        popupScale = null;
    }

    @SuppressWarnings("unused")
    private void onScaleCancelled(Popup popup) {
        clearMeasuringLine();
        popupScale = null;
    }

    private void measureLine(Point p0, Point p1) {
        if (measurementLine == null)
            measurementLine = new GraphicMeasurementLine();
        measurementLine.setPixelCoords(p0.x, p0.y, p1.x, p1.y);
        popupScale.setMeasuredDistance(Util.distance(p1, p0));
    }

    @SuppressWarnings("unused")
    private void onMaskCancelled(Popup popup) {
        params.trParams.setMask(maskPopup.getOldMask());
        maskPopup = null;
    }

    @SuppressWarnings("unused")
    private void onMaskOk(Popup popup) {
        params.trParams.setMask(maskPopup.getNewMask());
        maskPopup = null;
    }

    // =======================================================================
    // Private methods

    /**
     * Displays a single image.
     */
    private void displayFrame(int frameIndex, Mat frame) {
        if (frame != null) {
            Image image = Util.mat2BufferedImage(frame);
            runAndWait(() -> {
                imageView.imageProperty().set(image);
                frameNumLbl.setText("" + frameIndex);
                // Ugly hack to differentiate between user and programmatic slider changes
                ignoreSliderChanges = true;
                frameSlider.valueProperty().set(frameIndex);
                ignoreSliderChanges = false;
            });
        }
    }

//    private void copyMaskToClipboard(com.jim.tracking.Region mask) {
//        final ClipboardContent content = new ClipboardContent();
//        content.putString(mask.toJson());
//        Clipboard.getSystemClipboard().setContent(content);
//    }

    private void drawAnnotations(Mat feedbackImage) {
        // If scale is being measured...
        if (measurementLine != null)
            // Draw the line
            measurementLine.draw(feedbackImage);
        // If mask is being defined...
        if (maskPopup != null)
            maskPopup.annotate(feedbackImage);
    }

    private void disablePlaying() {
        playButton.setDisable(true);
        slowerBtn.setDisable(true);
        fasterBtn.setDisable(true);
        frameSlider.setDisable(true);
        msg.setDisable(true);
        params.grParams.running = false;
    }

    private void clearMeasuringLine() {
        measurer.stopMeasuring();
        measurementLine = null;
    }

    private void pausePlayingVideo() {
        updatePlayButton(false);
    }

    /**
     * Changes the size of the image view to fit the current video,
     * adjusted by the specified zoom factor.
     */
    private void adjustViewToVideo(double zoomLevel) {
            /* THIS DOESN"T WORK - need to rethink it
        if (zoomLevel != this.zoomLevel) {
            this.zoomLevel = zoomLevel;
            imageView.setFitWidth(this.videoPlayer.getfWidth() * zoomLevel);
            imageView.setFitHeight(this.videoPlayer.getfHeight() * zoomLevel);
        }
            */
    }

    /**
     * Adjusts the slider to display the frame number.
     * @param videoPlayer Camera/video to adjust to.
     */
    private void adjustSliderToVideo(VideoPlayer videoPlayer) {
        final long frames = videoPlayer.getNumOfFrames();
        frameSlider.setMin(1);
        frameSlider.setMax(frames);
        frameSlider.setMajorTickUnit(frames - 1);
    }

    private void updatePlayButton(boolean nowPlaying) {
        params.grParams.running = nowPlaying;
        this.playButton.setText(params.grParams.running ? "Pause" : "Play");
    }

    private Size getScaledVideoSize() {
        return params.srcParams.scaleSize(new Size(videoPlayer.getfWidth(), videoPlayer.getfHeight()));
    }

}
