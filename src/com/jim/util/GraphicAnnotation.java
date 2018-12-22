package com.jim.util;

import org.opencv.core.Mat;

/** Interface for objects which can draw on an OpenCV Mat. */
public interface GraphicAnnotation {
    void draw(Mat mat);
}
