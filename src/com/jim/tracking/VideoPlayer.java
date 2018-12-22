package com.jim.tracking;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;
import static org.opencv.videoio.Videoio.CAP_PROP_POS_FRAMES;

@SuppressWarnings("WeakerAccess")
public class VideoPlayer {
    private final VideoCapture camera;
    private final double fps;
    /** Original (unscaled) image/video size. */
    private final double fWidth;
    /** Original (unscaled) image/video size. */
    private final double fHeight;
    private final long numOfFrames;
    private final String videoFile;
    private int frameIndex;

    public VideoPlayer(String videoFile) {

        this.videoFile = videoFile;
        camera = new VideoCapture(videoFile);
        fps = camera.get(Videoio.CAP_PROP_FPS);
        fWidth = camera.get(CAP_PROP_FRAME_WIDTH);
        fHeight = camera.get(CAP_PROP_FRAME_HEIGHT);
        numOfFrames = (long) camera.get(CAP_PROP_FRAME_COUNT);
    }

    public boolean isOpened() {
        return camera.isOpened();
    }

    public boolean read(Mat frame) {
        if (camera.read(frame)) {
            frameIndex++;
            return true;
        }
        return false;
    }

    public void release() {
        camera.release();
    }
    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        // Set position to 1 before desired frame so next read gets the desired frame
        frameIndex--;
        this.frameIndex = frameIndex;
        camera.set(CAP_PROP_POS_FRAMES, frameIndex);
    }

    public double getFps() {
        return fps;
    }

    public double getfWidth() {
        return fWidth;
    }

    public double getfHeight() {
        return fHeight;
    }

    /** Returns width and height as a size. */
    public Size getSize() { return new Size(fWidth, fHeight); }

    public long getNumOfFrames() {
        return numOfFrames;
    }

    public String getVideoFile() {
        return videoFile;
    }
}
