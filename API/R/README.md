# R API for reading YetAnotherTracker output

This directory contains simple R scripts to assist with reading and processing the output from YetAnotherTracker. 
Functions exist to extract the longest track from a CSV output file, read the coordinates from a mask file, 
and plot all trajectories in a CSV file. 

These function make use of the [trajr](https://cran.rstudio.com/web/packages/trajr/vignettes/trajr-vignette.html) 
package internally, and simplify importing trajectories into `trajr`, but don't require you to use it.