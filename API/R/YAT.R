# Functions for reading the output and mask file from YetAnotherTracker.
# Trajectories are read in a format compatible with the trajr package.
# 
# @examples
# 
# # Example 1: plot a single trajectory and mask
# # There should be 1 long track in this file
# csvFile <- "S141_M1_OA_21_05_24_Trim.csv"
# trj <- TrajFromCoords(YATReadLongestTrackPoints(csvFile), timeCol = "Time")
# # Scale from pixels to mm
# scale <- 0.56
# trj <- TrajScale(trj, scale, "mm")
# 
# # We can make use of the mask
# arena <- YATReadMask(csvFile, scale)
# 
# # Plot trajectory and mask
# plot(trj)
# lines(arena, lwd = 2, col = "#802010")
# 
# Example 2: Plot all the trajectories in the file that are at least 100 mm long. This can be used to help diagnose tracking problems
# YATPlotTrjs(csvFile, minLengthToPlot = 100, blueIfShorterThan = 150)


library(trajr)
library(jsonlite)

# Structure of CSV file. This can be passed to TrajsBuild as the csvStruct argument
YAT_CSV_STRUCT <- list(x = "x", y = "y", time = "Time")

# Reads and returns the coordinates in the mask file for a video.
#
# Assumes that the mask file contains single polygon.
#
# @param csvFile Name of the CSV file used by YetAnotherTracker. The JSON file
#   is expected to have the same name but with extension "JSON".
# @param scale points are scaled by this value.
# @param failIfMissing If the JSON file doesn't exist, throws an error when
#   TRUE, return NULL when FALSE.
# @param flipCoords If TRUE, converts from the y-down coordinate system of
#   videos to the y-up coordinate system of R plots.
#
# @return data frame with 2 columns, x & y, containing the coordinates of the
#   mask polygon.
YATReadMask <- function(csvFile, scale = 1, failIfMissing = TRUE, flipCoords = TRUE) {

  maskFile <- gsub("csv$", "json", csvFile)
  
  # Does the mask file exist?
  if (!file.exists(maskFile)) {
    if (failIfMissing)
      stop(sprintf("The JSON mask file does not exist: %s", maskFile))
    return(NULL)
  }
  
  # Read the mask file
  mask <- fromJSON(maskFile)
  pts <- mask$points[[1]] # Assume there's only 1 polygon

  if (flipCoords) {
    # Convert the coordinate system by flipping vertically
    trjPoints <- read.csv(csvFile, comment.char = '#')
    maxY <- max(trjPoints$y)
    pts$y <- maxY - pts$y
  }
  
  # Close the polygon
  pts[nrow(pts) + 1, ] <- pts[1, ]
  # Scale
  pts <- pts * scale
  
  # Keep the includeRegion flag as an attribute
  attr(pts, "includeRegion") <- mask$includeRegion
  
  pts
}

# reads the masks for multiple videos, and returns them as a list of coordinate data frames.
YATReadMasks <- function(files, scales = 1, failIfMissing = TRUE, flipCoords = TRUE) {
  if (length(scales) == 1)
    scales <- rep(scales, times = length(files))
  lapply(seq_along(files), function(i) {
    YATReadMask(files[i], scales[i], failIfMissing, flipCoords)
  })
}

# Reads a set of points from a CSV file, and returns the points in the longest
# track. The file may contain multiple tracks due to noise in the video
# conversion process, or because there are multiple animals being tracked.
# 
# @param fileName name of CSV file containing coordinates output by YetAnotherTracker.
# @param flipCoords If TRUE, converts from the y-down coordinate system of
#   videos to the y-up coordinate system of R plots.
#
# @return A data frame of points with columns: 
#   Frame - the number of the frame.
#   Time - if YAT was given a frame rate for the video, this is the frame time
#      in seconds since the start of the video. 
#   TrackId - numeric id of the track output by YAT. Since this function returns 
#      points for a single track, all values will be the same. 
#   x, y - x and y-coordinate of points. Units depend on how YAT was run. 
#   ValueChanged - true if x, y has changed since last frame.
YATReadLongestTrackPoints <- function(fileName, flipCoords = TRUE) {

  # Picks the longest track and returns its track ID
  .pickLongestTrack <- function(points) {
    tids <- unique(points$TrackId)
    midx <- sapply(tids, function(tid) TrajLength(TrajFromCoords(points[points$TrackId == tid, ], "x", "y", "Time")))
    tids[which.max(midx)]
  }
  
  points <- read.csv(fileName, comment.char = '#')
  
  if (flipCoords) {
    # Convert the coordinate system by flipping vertically.
    # There is a trajectory with ID -1 which contains 1 point, the maximum extent of the x and y values.
    maxY <- max(points$y, na.rm = TRUE)
    points$y <- maxY - points$y
  }
  
  # Get the id of the longest track  
  tid <- .pickLongestTrack(points)
  
  # Just return points in the longest track
  track <- points[points$TrackId == tid, ]
  
  # Rearrange columns
  cols <- unique(c("x", "y", "Time", names(track)))
  
  track[, cols]
}


###### Data exploration ########

.identifyLongTracks <- function(points, minLength, minDuration) {
  tids <- unique(points$TrackId)
  msr <- sapply(tids,
                function(tid) {
                  trj <- TrajFromCoords(points[points$TrackId == tid, ], "x", "y", "Time")
                  # cat(sprintf("Track %d, length %g, duration %g\n", tid, TrajLength(trj), TrajDuration(trj)))
                  TrajLength(trj) >= minLength && TrajDuration(trj) >= minDuration
                })
  tids[which(msr)]
}

# Returns the track IDs of tracks in  with at least length \code{minLength} and
# duration \code{minDuration}. Units are the units in the CSV file.
YATIdentifyLongTracks <- function(csvFile, minLength, minDuration) {
  if (!file.exists(csvFile))
    stop(sprintf("File doesn't exist: %s", csvFile))
  
  points <- read.csv(csvFile, comment.char = '#')

  tids <- unique(points$TrackId)
  msr <- sapply(tids,
                function(tid) {
                  trj <- TrajFromCoords(points[points$TrackId == tid, ], "x", "y", "Time")
                  # cat(sprintf("Track %d, length %g, duration %g\n", tid, TrajLength(trj), TrajDuration(trj)))
                  TrajLength(trj) >= minLength && TrajDuration(trj) >= minDuration
                })
  tids[which(msr)]
}


# Plots multiple trajectories in the specified file, coloured according to some
# criterion.
YATPlotTrjs <- function(csvFile, flipCoords = TRUE, blueIfShorterThan = 1, plotMask = FALSE, minLengthToPlot = 0, minDurationToPlot = 0) {
  if (!file.exists(csvFile))
    stop(sprintf("File doesn't exist: %s", csvFile))

  points <- read.csv(csvFile, comment.char = '#')

  if (flipCoords) {
    # Convert the coordinate system by flipping vertically.
    # There is a trajectory with ID -1 which contains 1 point, the maximum extent of the x and y values.
    maxY <- max(points$y, na.rm = TRUE)
    points$y <- maxY - points$y
  }

  # What tracks should be plotted?
  tracksToPlot <- .identifyLongTracks(points, minLengthToPlot, minDurationToPlot)
  interestingTracks <- .identifyLongTracks(points, blueIfShorterThan, 1)
  
  # Calculate view extents
  xlim <- range(points$x[points$TrackId %in% tracksToPlot])
  ylim <- range(points$y[points$TrackId %in% tracksToPlot])
  plot(NULL, xlim = xlim, ylim = ylim, asp = 1, xlab = "x (m)", ylab = "y (m)", main = csvFile)
  
  if (plotMask) {
    lines(YATReadMask(csvFile, failIfMissing = TRUE, flipCoords = flipCoords), lwd = 2, col = "#20b090")
  }
  
  for (tid in tracksToPlot) {
    trj <- TrajFromCoords(points[points$TrackId == tid, ], "x", "y", "Time")
    isReal <- tid %in% interestingTracks
    col <- ifelse(isReal, "red", "blue")
    #col <- ifelse(isReal, "#ff000040", "#0000bb40")
    plot(trj, add = TRUE, start.pt.col = col, col = col)
    if (isReal) {
      text(trj[1, "x"], trj[1, "y"], labels = tid)
      cat(sprintf("%d length %g, %d points, frames %d - %d\n", 
                  tid, TrajLength(trj), nrow(trj),
                  trj$Frame[1], trj$Frame[nrow(trj)]))
    }
  }
}

