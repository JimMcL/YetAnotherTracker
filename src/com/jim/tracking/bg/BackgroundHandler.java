package com.jim.tracking.bg;

import com.jim.Params;
import org.opencv.core.Mat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jim_m on 14-May-17.
 */
public abstract class BackgroundHandler {
    /** Current background image. */
    Mat background = null;

    public abstract void processFrame(Mat frame);

    public Mat getBackground() {
        return background;
    }

    public boolean canHandleColour() {
        return false;
    }

    /* =================================================================== */

    static class FactoryInfo {
        String[] userArgs;
        String videoFile;
        Params params;

        public FactoryInfo(String[] userArgs, String videoFile, Params params) {
            this.userArgs = userArgs;
            this.videoFile = videoFile;
            this.params = params;
        }
    }

    protected interface BackgroundHandlerFactory {
        BackgroundHandler newHandler(FactoryInfo info);
    }
    private static Map<String, BackgroundHandlerFactory> factories = new HashMap<>();

    static void registerHandlerFactory(String name, BackgroundHandlerFactory factory) {
        factories.put(name, factory);
    }

    /** Factory method - returns a background handler.
     * @param descr Name and optional additional params (syntax "name:arg1:arg2") of the background handler to be returned.
     * @param videoFile Name of the file to construct a background for.
     * @param params User defined parameters. */
    public static BackgroundHandler getHandler(String descr, String videoFile, Params params) {
        String[] parts = descr.split(":");
        String name = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        BackgroundHandlerFactory factory = factories.get(name);
        if (factory == null)
            throw new RuntimeException("No such background handler: " + name + ", available handlers are " + factories.keySet());
        return factory.newHandler(new FactoryInfo(args, videoFile, params));
    }
}
