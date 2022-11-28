# README

A Java application for tracking moving objects in videos, based on OpenCV. It was built for extracting the paths of
spiders and insects from a video, and writing the coordinates to a CSV file.

This application is a toolkit, providing a means to chain together various computer vision techniques without coding. As
such, an understanding of computer vision techniques is very useful. Graphical output is provided as a
debugging/comprehension tool to aid in determining the best parameters for a task.

It works by defining the algorithms and parameters to be used, then applying them to a video. There is no interactive
control; you just define everything then let it run. By default, you can watch what is happening, however if you are
confident that it will run correctly, the option `--headless` runs the tracking in the background with no user interface
at all. This is the fastest way to run tracking.

There will usually be many false tracks generated as a result of noise, physical vibrations or lighting fluctuations.
These false tracks generally start and end in roughly the same location, so real tracks may be identifiable by their
long length, maximum displacement or diffusion distance or maximum number of points in the track (although clearly none
of these are infallible tests, so the best approach will depend on the nature of your tracks).

Currently, the app does not have a pretty GUI interface, rather it is controlled through command line parameters and/or
a configuration file. To use it, you should be comfortable with editing text files and know what a command line
application is and how to use it.

The app has the very early beginnings of a GUI which is largely non-functional. There are some useful components:
the `Pause` button and the `Slower` and `Faster` buttons are functional, although the speed is limited by the processing
requirements for each frame, so you might not see any speedup from the `Faster` button. The `Frame` slider is useful as
a visual aid only; it cannot be manipulated. The `Scale` and `Mask` buttons are functional as described below, and are
used to interactively define a region of interest or the display scale.

The right-hand panel is mainly empty, but can be used to interactively experiment with the contour filter
settings `min-contour`, `max-contour`, `min-contour-length` and `max-contour-length`.

There is some basic R functionality to simplify working with the output from this program in 
[R](https://www.r-project.org/) in [API/R](API/R).   

<a id="Installation"></a>

## Installation

* Download this application. Alternatively, just download the jar file
  ([YetAnotherTracker.jar](https://github.com/JimMcL/YetAnotherTracker/blob/master/out/artifacts/YetAnotherTracker_jar/YetAnotherTracker.jar))
  and the bat
  file ([YetAnotherTracker.bat](https://github.com/JimMcL/YetAnotherTracker/raw/master/YetAnotherTracker.bat)).
* If you have not previously done so, install Java.
* Download the OpenCV library (https://opencv.org/), version 4.5.2.
* On Windows, edit `YetAnotherTracker.bat` to specify the locations of OpenCV. See comments within the file for details. Run it with
  no arguments for a usage message.

## Running the application

### Windows

The bat file `YetAnotherTracker.bat` may be used to run the application on Windows.

You can safely ignore the warning:
`WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0x80000002. Windows RegCreateKeyEx(...) returned error code 5.
`.

### Processing overview

Frames from the [input video](#Input) are processed in order. Moving objects
are [detected in each frame](#MotionDetector), using one of several possible techniques. Tracks are created
by [combining close objects](#Tracks) from different frames. Tracks may be written to an [output CSV file](#Output). All
processing is controlled by specifying options on the command line and/or in a defaults file. Options specified on the
command line override those in a defaults file.

### What options should I use?

Options can be specified on the command line using the long option syntax e.g. `--output file`. Some options also have a
short option syntax, e.g. `-o file`. Options can also be specified in the defaults file (see option `defaults`), which
has a single option per line, with the syntax e.g. `output=file`.

<a id="Input"></a>

#### Input files

* `--defaults <file>` specifies the name of an defaults file which contains any option values. Ignored if specified in
  the defaults file.
* `--video <file>` specifies the input video file name. Alternatively, just add the file name to the command line
  without an option.
* `--view-rotation {0|90|180|-90}` angle to rotate video before processing (but after optional resizing).
* `--resize <size>` resizes the input video to have the specified width. This affects all subsequent operations, in
  particular, tracking parameters, scale etc. will have different effects on differently sized videos. Scaling the video
  to a smaller size should speed up processing, but may lose detail.
* `--fps <fps>` overrides the frame rate specified in the video metadata, and is used to calculate the time for each
  frame. This option is useful if your camera records playback speed rather than recording speed in the video metadata,
  or is otherwise incorrect.

Output trajectory coordinates are in pixels unless you specify a scale. To specify a scale, you must know either the
scale _after resizing_, e.g. `27 pixels/mm`, or the real world width or height of the video frame, e.g. `600 mm`. You
can use the `Scale`
button within the app to help calculate the scale by allowing you to measure a distance in the video.
Setting `--view scale ?` will automatically turn off autorun and display the scale measurement dialog box.

* `--view-scale <number>` specifies the display scale _after resizing_ (see the `--resize` option).
* `--view-width <number> <units>`, `--view-height <number> <units>` width/height of video frame in real world units,
  eg. `view-width=600mm`. You must specify either the view size or view scale if you want the output CSV file in real
  world units rather than pixels (see option `csv-units`).

<a id="Output"></a>

#### Output files

Track positions can be saved in a CSV file. Columns are:

| Column | Description |
| ------ | ----------- |
| `Frame` | 0-based video frame number |
| `Time` | frame time in seconds, calculated as `frame number / fps` |
| `TrackId` | A file can contain multiple tracks, each is given a unique numeric ID |
| `x`, `y` | X & y position of the track in the frame. Units are pixels or world units specified by options `csv-units` and `view-scale`, `view-width` or `view-height` |

Additionally, the contents of the main window can be written to a video file.

* `-o <file>`, `--output <file>` specifies the name of the output CSV or video file. The file type is deduced from the
  file extension.
* `--csv` Write a CSV file. The CSV file name is the same as the name of the input video file, with the extension
  changed to `.csv`.
* `--csv-comment-prefix <prefix>` writes the runtime parameters to the CSV file, prefixed by the specified character.
* `--csv-units` specifies the x, y units in the output CSV file. May be either "pixels" (or "px", the default), or else
  scaled to the units specified in the options `view-scale`, `view-width` or `view-height`.
* `--output-all-frames` if specified, positions for all frames are written to the CSV file. By default, duplicated
  positions are not written to the file.

<a id="Debugging"></a>

#### Debugging

* `--autorun {true|false}` starts or stops the video from playing automatically on app startup.
* `-v`, `--verbose` writes various status information to the console.
* `-d`, `--debug` writes very verbose debugging output to the console, which can be useful e.g. to determine why
  contours aren't converted to tracked objects.
* `--frame-size <width>x<height>` resizes the main window to the specified size.
* `--headless` runs without any kind of user interface. This is useful if you know you have specified all the correct
  parameters and just want to process a video as fast as possible.

### Region of interest

You can exclude parts of the video from analysis by defining a mask, which is simply one or more polygons. The mask may
define the regions to be processed, or the regions to be excluded from processing. The polygons are defined in a file
using the
[JSON](https://www.json.org/) format, specified with the option `--mask-file <json>`. The file should contain 2
values, `includeRegion` (boolean) and `points` (array of arrays of points which are objects with `x` and `y` values).
Each array of points defines a polygon. Points use the coordinate system of the untransformed video, i.e. pixels.

The `Mask` button may be used to create a mask by drawing it on the video. Click the `Save` button to write the mask
JSON to a file with the same name as the input video file, and extension `.json`. Be aware that changing a mask while
analysing can lead to some surprising artifacts. It is best to define a new mask, save it to a file, then restart the
analysis specifying the new mask file. As a convenience, the command line option `--mask true` will use a mask file with
name `<video file>.json` if it exists, and silently do nothing if the file doesn't exist.

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

<a id="MotionDetector"></a>

### Motion detector

Two types of motion detector are available, "optical-flow" (the default), and "differences", specified by the
option `--motion-detector`.

#### Optical flow motion detector

`--motion-detector optical-flow`

There are no options to customise the optical flow motion detector. Specify the option
`--display-flow` to draw objects detected by the optical flow motion detector on the feedback window. This works by
first detecting potential features using Shi-Tomasi feature detection, then applying the Lucas-Kanade method with
pyramids.

#### Differences motion detector

`--motion-detector differences`

The differences motion detector requires a number of parameters to be specified; firstly a method for segmenting foreground from background.

`--foreground-segmenter background-subtraction`

Background subtraction works by subtracting a greyscale background from each frame (also converted to greyscale). The
background is constructed based on the option `--background-method`, and is created by averaging 1 or more frames which
are first converted to greyscale then Gaussian blur is applied (blur size is controlled by the option `--blur-size n`
, `--blur-size 0` prevents blur from being applied). Background method must be one of `FirstFrames:n`, `none`
, `PreviousFrames:n`, or `FullMovie`, where `n` is the number of frames to be averaged. The subtracted image is then
thresholded: pixel values greater than the threshold are considered foreground. Background method `none` performs no
background subtraction.

The threshold method must be one of `adaptive` (the default), `otsu` or `global`, as specified by the
option `--threshold-method <method>`. Adaptive mean thresholding varies the threshold based on a region of pixels
(OpenCV function adaptiveThreshold). You can vary the results of adaptive thresholding by specifying a constant
`C` (option `-C n` or `--threshold-C n`) which is subtracted from the calculated threshold, and the block size to be
used (option `-B n` or `--threshold-blocksize n`, default block size is 5). Other threshold methods are `GLOBAL` which simply applies a global
threshold intensity value (specified by the option `--threshold n`), and `OTSU`
which attempts to calculate a suitable threshold value. Specify `--threshold-invert` to treat dark areas as foreground and light areas as background.

The options `--display-background` and `--display-subtraction` can be specified to display the results of background
construction and background subtraction respectively.

    --foreground-segmenter KNN
    --foreground-segmenter KNN:<history>:<dist2Threshold>:<detectShadows>

Uses a K-nearest neighbours - based Algorithm
(see the OpenCV class `cv::bgsegm::BackgroundSubtractorKNN`). The `history` parameter specifies how many past frames are
used to calculate the background.
`dist2Threshold` varies the level used to differentiate between foreground and background.
`detectShadows` should be `true` or `false`. When `true`, a more accurate shape may be obtained from good quality
videos, but `false` may be more suitable when foreground and background are difficult to differentiate. Experiment with
different values, and use the `--display-threshold`
option to visualise the results.

    --foreground-segmenter MOG
    --foreground-segmenter MOG:<history>:<varThreshold>:<detectShadows>

Uses a Gaussian Mixture-based Background/Foreground Segmentation Algorithm
(see the OpenCV class cv::bgsegm::BackgroundSubtractorMOG). The parameters are much the same for `MOG` as for `KNN`.

After foreground segmentation (using either background subtraction, MOG or KNN), the foreground regions may expanded ("
dilated") to merge close small regions, and/or contracted. The option `--dilation-erosion <list>` specifies a
comma-separated list of dilation or erosion sizes. Negative values indicate erosion, positive values indicate dilation.
For example, the value `4,-2` will dilate by 4 pixels then erode by 2.

`--display-threshold` displays the result after segmentation and dilation.

### Contour filtering

Next, contours are constructed around the forground objects, and any contours whose areas are less than `--min-contour`
or greater than `--max-contour` are discarded. Contours can be filtered out based on their perimeter lengths which must
be between `min-contour-length` and `max-contour-length` (if specified). The relationship between contour area and
length allows a crude filter on shape. Any contours after the first 50 are also discarded.

Use `--display-contours` to display contours on the feedback window, and `--display-rectangle` to display contour
bounding rectangles.

Tracked objects are created from the contour centroids. Specify option `--display-centroid` to draw circles around contour centroids.
___

<a id="Tracks"></a>

### Creating tracks

To create tracks, detected objects must be combined across frames. A Kalman filter (option `-k <arg>`, `--kalman <arg>`)
is used to combine detected objects across frames into tracks. The filter `<arg>` can be one of `veryfast`, `fast`
, `normal`, `slow`, or `veryslow`. The maximum distance (in pixels) is specified with the option `--max-jump <arg>`. If
an object moves further than the maximum distance between two consecutive frames, it will be treated as two separate
objects. Multiple objects within `--min-gap <arg>` units will be treated as a single object.

Tracks are matched with the closest detected objects in each frame. Use the option `--age-weighting <weight>` to match
objects to active tracks in preference to old, stopped, tracks. The `<weight>` value is multiplied by the 'age' of the
track
(number of frames) since last detection to obtain a weighted distance between the object and the track.

You can prevent tracking from occurring during the starting frames with the option `--first-tracking-frame <frame>`,
which may be useful if the start of the video is noisy or the camera is settling. Similarly, the
option `--no-tracks-after <frame>` prevents any new tracks from being created after the specified frame.

You can delete tracks that have not moved after some number of frames by setting `--retirement-age <frames>`. This may
be useful for videos that are noisy at the start, creating a lot of tracks that never move again.

By default, when a track leaves the screen, it is treated as though it stopped where it disappeared. If you specify a
positive value for `--termination-border <pixels>`, tracks which stop within the specified number of pixels of the mask
region will be terminated.

Specify `--display-tracks` to draw a cross at each track current location. Cross colour indicates the track ID.

### But again, what options should I use?

Best results will be obtained by experimenting with the various settings, and will entirely depend on the video.

If the video is high quality, consistently well lit and in focus, with clear contrast between the object to be tracked
and the background, then the simplest options may give good results. Try
`--motion-detector differences`, `--background-method none` and
`--threshold-method global` and fiddle with the value of `--threshold
<n>` until you get a reasonable result. Also try different blur sizes, including 0.

If the intensity of lighting varies across the frame, try
`--threshold-method adaptive` and vary `--threshold-C <n>`.

For more difficult videos, try `--motion-detector optical-flow`, then
`--motion-detector differences` with `--foreground-segmenter KNN:<history>:<dist2Threshold>:<detectShadows>`. Fiddle
with
`<history>` (the number of frames used to construct the background),
`dist2Threshold` (threshold value between foreground and background), and `detectShadows` (`true` or `false`: if `true`,
shadows are detected and not treated as part of the foreground).

While playing with thresholding, use `--display-threshold` to visalise the results.

Use `--min-contour` and `--max-contour` to control what contours are kept as potential objects to be tracked, and
use `--display-contours` to see the results.

Contours which lie within another contour are not detected. If you have an object which is not being detected even
though it appears in the threshold window, try masking an area to exclude a containing contour.

If you want tracks as output, you must specify `--kalman <speed>`.

___

## TODO

* Apply image stabilisation. This might even allow handheld videos to be used effectively
* Improve the algorithm used for matching detected objects in each frame to existing tracks.
* Apply some kind of object detector such as YOLO to detect (and identify) foreground objects.
