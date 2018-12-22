package com.jim.tracking;

import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

public class CameraInfo {

    public final VideoCapture camera;
    public final double fps;
    public final double fWidth;
    public final double fHeight;
    public final long numOfFrames;

    public CameraInfo(VideoCapture camera) {
        this.camera = camera;
        fps = camera.get(Videoio.CAP_PROP_FPS);
        fWidth = camera.get(CAP_PROP_FRAME_WIDTH);
        fHeight = camera.get(CAP_PROP_FRAME_HEIGHT);
        numOfFrames = (long) camera.get(CAP_PROP_FRAME_COUNT);
    }

    public boolean isOpen() {
        return camera != null && camera.isOpened();
    }
}