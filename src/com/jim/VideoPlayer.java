package com.jim;

import com.jim.tracking.CameraInfo;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;

import static org.opencv.videoio.Videoio.CAP_PROP_POS_FRAMES;

public class VideoPlayer {

    private final CameraInfo cameraInfo;
    private int frameIndex = 0;

    public VideoPlayer(String videoFile) {
        VideoCapture camera = new VideoCapture(videoFile);
        if (!camera.isOpened()) {
            System.err.println("Unable to open video file '" + videoFile + "'");
            if (new File(videoFile).exists() && new File(videoFile).canRead())
                System.err.println("Is the opencv ffmpeg DLL (eg opencv_ffmpeg320_64.dll) in your path?");
        }
        cameraInfo = new CameraInfo(camera);
    }

    public boolean isOpen() {
        return cameraInfo.isOpen();
    }

    public CameraInfo getCameraInfo() {
        return cameraInfo;
    }

    /** Reads and returns the next frame.
     * @return A {@link Mat} or {@code null} if another frame is not available.
     */
    public Mat nextFrame() {

        frameIndex++;
        Mat frame = new Mat();
        return cameraInfo.camera.read(frame) ? frame : null;
    }

    /** Sets the index of the frame to be returned by the next call to {@link #nextFrame}. */
    public void gotoFrame(int nextFrameIndex) {
        frameIndex = nextFrameIndex - 1;
        cameraInfo.camera.set(CAP_PROP_POS_FRAMES, frameIndex);
    }

    public int getFrameIndex() {
        // Note that cameraInfo.camera.get(CV_CAP_PROP_POS_FRAMES) sometimes return 0 incorrectly,
        // so don't use it.
        return frameIndex;
    }
}
