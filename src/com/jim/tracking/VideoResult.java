package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoWriter;

import java.util.List;

/**
 * Created by jim_m on 24-May-17.
 */
public class VideoResult implements MotionDetector.Filter {
    private VideoWriter writer;
    private String fileName;
    private Params.GraphicParams opt;

    public VideoResult(String fileName, Params.GraphicParams opt) {
        this.fileName = fileName;
        this.opt = opt;
    }

    @Override
    public void handle(List<MotionDetector.DetectedObject> detectedObjects, List<TrackWithEllipse> tracks, Mat greyFrame, Mat feedbackImage, Params opts, VideoPlayer camera) {
        // The "if" is a bit of a hack - the feedback frame is (possibly) the wrong size until tracking has started
        if (feedbackImage.size().equals(opt.playbackSize))
            writer.write(feedbackImage);
    }

    @Override
    public void onCameraOpened(VideoPlayer camera) {

        double fps = camera.getFps();
//        int fourcc = (int) camera.get(Videoio.CAP_PROP_FOURCC);
        // Requires AVI rather than mp4, but decent quality
        final int fourcc = VideoWriter.fourcc('X', 'V', 'I', 'D');
//        final int fourcc = VideoWriter.fourcc('H', '2', '6', '4');
//        final int fourcc = VideoWriter.fourcc('X', '2', '6', '4');
//        final int fourcc = VideoWriter.fourcc('L', 'A', 'G', 'S');
        writer = new VideoWriter(fileName, fourcc, fps, opt.playbackSize, true);
    }

    @Override
    public void onDone(VideoPlayer camera) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "- write output video to " + fileName;
    }
}
