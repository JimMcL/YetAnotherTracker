package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.merge;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2YCrCb;
import static org.opencv.imgproc.Imgproc.COLOR_YCrCb2BGR;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Reads frames sequentially from a video file, potentially transforms them, then passes them to a handler.
 */
public class FrameLoop {
    public interface Handler {
        void onVideoOpened(VideoPlayer camera);
        void onFrame(Mat greyFrame, Mat colourFrame) throws IOException;
        void onDone() throws IOException;
    }


    public void run(Handler handler, String videoFile, Params.GraphicParams grParams, Params.SrcVideoParams srcParams, Params.TrackerParams params) throws IOException {

        VideoPlayer player = new VideoPlayer(videoFile);
        if (!player.isOpened()) {
            System.err.println("Unable to open video file '" + videoFile + "'");
            System.err.println("Is the opencv ffmpeg DLL (eg opencv_ffmpeg320_64.dll) in your path?");
            return;
        }

        handler.onVideoOpened(player);

        Mat rawFrame = new Mat();

        // Frame size
        Size sz = null;
        Mat frame = null;

        // For each frame in the video...
        while (true) {
            // Maybe read the next frame
            if (grParams.running || player.getFrameIndex() == 0) {
                if (!player.read(rawFrame)) {
                    // Read failed - stop
                    break;
                }
            }
            // Clone the raw frame because the frame object gets modified
            if (rawFrame.size().width > 0) // rawFrame is empty at EOF
                frame = rawFrame.clone();
            sz = transformFrame(srcParams, sz, frame);

            // Maybe equalize histogram
            if (params.equalize)
                equalize(frame, frame);

            // Convert to greyscale
            Mat greyFrame = new Mat(frame.size(), CvType.CV_8UC1);
            cvtColor(frame, greyFrame, Imgproc.COLOR_BGR2GRAY);
            // Blur to reduce noise
            if (params.blurSize > 0)
                Imgproc.GaussianBlur(greyFrame, greyFrame, new Size(params.blurSize, params.blurSize), 0);

            // Do something with the frame
            handler.onFrame(greyFrame, frame);
        }
        player.release();

        handler.onDone();
    }

    private Size transformFrame(Params.SrcVideoParams srcParams, Size sz, Mat frame) {
        // Perhaps rotate
        srcParams.angle.rotate(frame);
        // Resize
        if (sz == null)
            // Calculate desired frame size
            sz = srcParams.scaleSize(frame.size());
        Imgproc.resize(frame, frame, sz);
        return sz;
    }

    private void equalize(Mat src, Mat dest) {
        if(src.channels() >= 3)
        {
            // To equalize a colour image without messing up the colours,
            // convert to YCbCr, equalize Y, then convert back to BGR
            // http://stackoverflow.com/a/15009815
            Mat ycrcb = new Mat();

            cvtColor(src, ycrcb, COLOR_BGR2YCrCb);
            List<Mat> channels = new ArrayList<>();
            Core.split(ycrcb, channels);

            Imgproc.equalizeHist(channels.get(0), channels.get(0));

            merge(channels,ycrcb);

            cvtColor(ycrcb, dest, COLOR_YCrCb2BGR);
        }
    }
}
