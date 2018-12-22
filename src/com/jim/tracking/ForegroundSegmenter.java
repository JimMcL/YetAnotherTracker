package com.jim.tracking;

import com.jim.Params;
import org.opencv.core.Mat;

public interface ForegroundSegmenter {
    Mat segment(int frameIndex, Mat greyFrame, Mat colourFrame, Params params);
}
