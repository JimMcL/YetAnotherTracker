# README

A Java application for analysing motion in videos, based
on OpenCV. It was built for extracting the paths of spiders and
insects from a video, and writing the coordinates to a CSV file.

This application is a toolkit, providing a means to chain together 
various computer vision techniques without coding. As such, an understanding 
of computer vision techniques is very useful. Graphical output is provided as a
debugging/comprehension tool to aid in determining the best parameters
for a task.

There will usually be many false tracks generated as a result of noise,
physical vibrations or lighting fluctuations. These false tracks
generally start and end in roughly the same location, so real tracks
may be identifiable by their large diffusion distance or length (although clearly
this is not an infallible test and depends on the tracks).

Currently, the app is controlled through command line parameters.

## Installation

 * Download this application.
 * If you have not previously done so, install Java.
 * Install the OpenCV library (https://opencv.org/).

## Running the application

### Windows

The bat file `YetAnotherTracker.bat` may be used to run the application on
Windows. Edit the file to specify the locations of the Java
runtime and OpenCV. See comments within the file for details. Run it with no
arguments for a usage message.

### What options should I use?

Options can be specified on the command line using the long option syntax e.g. `--output file`. 
Some options also have a short option syntax, e.g. `-o file`. 
Options can also be specified in the defaults file (see option `defaults`), which has a single option per line, with the syntax e.g. `output=file`.
 
#### Input files
* `--defaults <file>` specifies the name of an defaults file which contains any option values.
* `--video <file>` specifies the input video file name. Alternatively, just add the file name to the command line without an option.
* `--view-rotation {0|90|180|-90}` angle to rotate video before processing (but after optional resizing).
* `--resize <size>` resizes the input video to have the specified width. This affects all subsequent operations, 
   in particular, tracking parameters, scale etc. will have different effects on differently sized videos. 
   Scaling the video to a smaller size should speed up processing, but may lose detail.  
* `--fps <fps>` overrides the frame rate specified in the video metadata, and is used to calculate 
   the time for each frame. This option is useful if your camera records playback speed rather than 
   recording speed in the video metadata, or is otherwise incorrect.

Output trajectory coordinates are in pixels unless you specify a scale. 
To specify a scale, you must know either the scale _after resizing_, e.g. `27 pixels/mm`, 
or the real world width or height of the video frame, e.g. `600 mm`. You can use the `Scale` 
button within the app to help calculate the scale. Setting `--view scale ?` will automatically 
turn off autorun and display the scale measurement dialog box. 

* `--view-scale <number>` specifies the display scale _after resizing_ (see the `--resize` option).    
* `--view-width <number> <units>`, `--view-height <number> <units>` width/height of video frame in real world units, eg. `view-width=600mm`. 
   You must specify the view size if you want the output CSV file in real world units rather than pixels (see option `csv-units`).

#### Output files  
Track positions can be saved in a CSV file. Columns are:  

| Column | Description |
| ------ | ----------- |
| `Frame` | 0-based video frame number |
| `Time` | frame time in seconds, calculated as `frame number / fps` |
| `TrackId` | A file can contain multiple tracks, each is given a unique numeric ID |
| `x`, `y` | X & y position of the track in the frame. Units are pixels or world units specified by options `csv-units` and `view-scale`, `view-width` or `view-height` |

Additionally, the contents of the feedback window (see option `--display-video` under 
[Debugging](#Debugging)) can be written to a video file.   

* `-o <file>`, `--output <file>` specifies the name of the output CSV or video file. The file type is deduced from the file extension.
* `--csv` Write a CSV file. The CSV file name is the same as the name of the input video file, with the extension changed to `.csv`.
* `--csv-comment-prefix <prefix>` writes the runtime parameters to the CSV file, prefixed by the specified character.
* `--csv-units` specifies the x, y units in the output CSV file. May be either "pixels" (or "px", the default), 
or else scaled to the units specified in the options `view-scale`, `view-width` or `view-height`.   
* `--output-all-frames` if specified, positions for all frames are written to the CSV file. By default, duplicated positions are not written to the file. 


<a id="Debugging"></a>
#### Debugging

* `--autorun [true|false]` starts or stops the video from playing automatically on app startup.
* `-v`, `--verbose` writes various status information to the console.
* `-d`, `--debug` writes very verbose debugging output to the console, which can be useful e.g. 
    to determine why contours aren't converted to tracked objects.
* `-g`, `--display-video` displays the (resized) video in a window, optionally with various overlays 
    based on other options. By default, the window is the size of the input video after rotation and resizing. 
  * `-s`, `--playback-speed` specifies a playback speed relative to "normal" - which is read from the 
    meta data in the video. Note that processing speeds may result in playback slower than specified.
  * `--frame-size <width>x<height>` resizes the playback window to the specified size. 

### Region of interest
You can exclude parts of the video from analysis by defining a mask, which is simply one 
or more polygons. The mask may define the regions to be processed, or the regions to be 
excluded from processing. The polygons are define in a file using the
[JSON](https://www.json.org/) format, specified with the option `--mask-file <json>`. 
The file should contain 2 values, `includeRegion` (boolean) and `points` (array of arrays 
of points which are objects with `x` and `y` values). Each array of points defines a polygon.
Points use the coordinate system of the untransformed 
video. 

The `Mask` button may be used to create a mask by drawing it on the video. Click the `Save` button 
to write the mask JSON to a file with the same name as the input video file, and extension `.json`.
be aware that changing a mask while analysing can lead to some surprising artifacts. It is best to define 
a new mask, save it to a file, then restart the analysis specifying the new mask file.
   
Example mask file:

    {"includeRegion":true,
     "points":
     [
         [{"x":1280.0,"y":70.0},
          {"x":1080.0,"y":35.0},
          {"x":970.0,"y":0.0},
          {"x":0.0,"y":0.0},
          {"x":0.0,"y":420.0},
          {"x":1280.0,"y":600.0}]
     ]
    }

___

#### Motion detector
Two types of motion detector are available, "optical-flow" (the default), and "differences", 
specified by the option `--motion-detector`.

##### Optical flow motion detector 

There are no options to customise the optical flow motion detector. Specify the option 
`--display-flow` to draw objects detected by the optical flow motion detector on the feedback window. 
This works by first 
detecting potential features using Shi-Tomasi feature detection, then applying the 
Lucas-Kanade method with pyramids.   

##### Differences motion detector

`--foreground-segmenter background-subtraction`

Background subtraction works by subtracting a greyscale background from each frame (also converted to 
greyscale). The background is constructed based on the option `--background-method`, and is created by 
averaging 1 or more frames which are first converted to greyscale then Gaussian blur is applied (blur size is controlled 
by the option `--blur-size n`, `--blur-size 0` prevents blur from being applied). Background method must be one of `FirstFrames:n`, `none`, `PreviousFrames:n`, 
or `FullMovie`, where `n` is the number of frames to be averaged.  The subtracted 
image is then thresholded: pixel values greater than the threshold are considered 
foreground. 

The threshold method must be one of `adaptive` (the default), `otsu` or `global`, as specified by the 
option `--threshold-method <method>`. 
Adaptive mean thresholding varies the threshold based on a region of pixels 
(OpenCV function adaptiveThreshold). You can vary the results of adaptive thresholding by specifying a constant 
`C` (option `-C n` or `--threshold-C n`) which is subtracted from the calculated threshold, and the block 
size to be used (option `-B n` or `--threshold-blocksize n`). Other threshold methods are `GLOBAL` which 
simply applies a global threshold intensity value (specified by the option `--threshold n`), and `OTSU` 
which attempts to calculate a suitable threshold value.

The options `--display-background` and `--display-subtraction` can be specified to display the results of 
background construction and background subtraction respectively.  

    --foreground-segmenter KNN
    --foreground-segmenter KNN:<history>:<dist2Threshold>:<detectShadows>

Uses a K-nearest neighbours - based Algorithm 
(see the OpenCV class `cv::bgsegm::BackgroundSubtractorKNN`).
The `history` parameter specifies how many past frames are used to calculate the background. 
`dist2Threshold` varies the level used to diffeentiate between foreground and background.
`detectShadows` should be `true` of `false`. When `true`, a more accurate shape may be obtained 
from good quality videos, but `false` may be more suitable when foreground and background are
difficult to differentiate. Experiment with different values, and use the `--display-trheshold` 
option to visualise the results. 

    --foreground-segmenter MOG
    --foreground-segmenter MOG:<history>:<varThreshold>:<detectShadows>

Uses a Gaussian Mixture-based Background/Foreground Segmentation Algorithm 
(see the OpenCV class cv::bgsegm::BackgroundSubtractorMOG). 
The parameters are much the same for `MOG` as for `KNN`.


After foreground segmentation (using either background subtraction, MOG or KNN), the foreground regions 
may expanded ("dilated") to merge close small regions, 
and/or contracted. The option `--dilation-erosion <list>` specifies a comma-separated list of 
dilation of erosion sizes. Negative values indicate erosion, positive values indicate dilation. For example, 
the value `4,-2` will dilate by 4 pixels then erode by 2.
 
`--display-threshold` displays the result after segmentation and dilation.

Next, contours are constructed around the forground objects, and any contours whose 
areas are less than `--min-contour` or greater than `--max-contour` are discarded. Any contours
after the first 50 are also discarded. Use `--display-contours` to display contours on the 
feedback window. Use `--display-rectangle` to display contour bounding rectangles.

Tracked objects are created from the contour centroids. 
___

### Creating tracks

To create tracks, detected objects must be combined between frames. 
A Kalman filter (option `-k <arg>`, `--kalman <arg>`) is used to combine detected objects across frames into 
tracks. The filter `<arg>` can be one of `veryfast`, `fast`, `normal`, `slow`, or `veryslow`. 
The maximum distance (in pixels) is specified 
with the option `----max-jump <arg>`. If an object moves further than the maximum distance between 
two consecutive frames, it will be treated as two separate objects. 
Multiple objects within `--min-gap <arg>` units 
will be treated as a single object. You can ignore the starting frames using the 
option `--first-tracking-frame <frame>`, which may be useful if the start of the video is noisy or 
the camera is settling. Similarly, the option `--no-new-tracks` prevents any new tracks from being created 
after the starting frame.

By default, when a track leaves the screen, it is treated as though it stopped where it disappeared.
If you specify a positive value for `--termination-border <pixels>`, tracks which stop
within the specified number of pixels of the mask region will be terminated.
 
### But again, what options should I use?

Best results will be obtained by experimenting with the various settings, 
and will entirely depend on the video. 
Sometimes it is best to start as simply as possible. First try the 
`--motion-detector optical-flow`, and only if that doesn't work, try 
`--motion-detector differences`. Start with 
`--background-method none` and `--threshold-method global` and fiddle with the value 
of `--threshold <n>` until you get a reasonable result. Try different blur sizes, including 0. 
If that doesn't produce a usable 
result, start experimenting with different background methods 
or foreground segmenters. While playing with thresholding, use `--display-threshold` to see
the results. 

Use `--min-contour` and `--max-contour` to control what contours are kept as 
potential objects to be tracked, and use `--display-contours` to see the results.   

If you want tracks as output, you must specify `--kalman <speed>`.
