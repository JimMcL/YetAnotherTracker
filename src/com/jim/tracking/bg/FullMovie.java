package com.jim.tracking.bg;

import com.jim.tracking.FrameLoop;
import com.jim.tracking.VideoPlayer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * Created by jim_m on 15-May-17.
 */
public class FullMovie extends BackgroundHandler {

    private Mat fullSize;

    public static void register() {
        BackgroundHandler.registerHandlerFactory("FullMovie", arg -> new FullMovie(arg));
    }

    public FullMovie(FactoryInfo arg) {
        try {
            fullSize = averageMovie(arg);
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private Mat averageMovie(FactoryInfo arg) throws IOException {

        final Mat[] avg = {null};

//        while (camera.read(frame)) {
//            Mat greyFrame = new Mat(frame.size(), CvType.CV_8UC1);
//            Imgproc.cvtColor(frame, greyFrame, Imgproc.COLOR_BGR2GRAY);
//
//            if (avg[0] == null)
//                avg[0] = Mat.zeros(frame.size(), CvType.CV_64F);
//            Imgproc.accumulateWeighted(greyFrame, avg[0], 1.0 / numFrames);
//        }
//
//        camera.release();

        FrameLoop.Handler handler = new FrameLoop.Handler() {
            private double numFrames;

            @Override
            public void onVideoOpened(VideoPlayer player) {
                numFrames = player.getNumOfFrames();
            }

            @Override
            public void onFrame(Mat greyFrame, Mat colourFrame) throws IOException {
                if (avg[0] == null)
                    avg[0] = Mat.zeros(greyFrame.size(), CvType.CV_64F);
                Imgproc.accumulateWeighted(greyFrame, avg[0], 1.0 / numFrames);
            }

            @Override
            public void onDone() throws IOException {
            }
        };

        new FrameLoop().run(handler, arg.videoFile, arg.params.grParams, arg.params.srcParams, arg.params.trParams);

        return avg[0];
    }

    @Override
    public void processFrame(Mat frame) {
        // Resize if required
        if (background == null) {
            Mat r = new Mat();
            fullSize.convertTo(r, CvType.CV_8U);
            Imgproc.resize(r, r, frame.size());
            background = r;
            fullSize.release();
            fullSize = null;
        }
    }
}
