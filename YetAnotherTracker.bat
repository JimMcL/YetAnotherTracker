@ECHO OFF
@REM Simple wrapper to run the app.
@REM All command line arguments are just passed to the app.
@REM
@REM System requirements:
@REM Java
@REM OpenCV
@REM



@REM --- Configurable variables ---

@REM The jar file includes the OpenCV jar file. You need to specify where OpenCV
@REM is located so that the runtime library can be found

@REM openCV location
set OCVD=C:\Jim\products\opencv-452

@REM Java location
set JAVA=java

@REM --- End configurable variables ---

set OPENCVLIBDIR=%OCVD%\build\java\x64

set YAT=%~dp0

@REM Try to find the jar file
if exist %YAT%\out\artifacts\YetAnotherTracker_jar\YetAnotherTracker.jar (
  set JAR=%YAT%\out\artifacts\YetAnotherTracker_jar\YetAnotherTracker.jar
)
if exist %YAT%\YetAnotherTracker.jar (
  set JAR=%YAT%\YetAnotherTracker.jar
)

"%JAVA%" "-Djava.library.path=%OPENCVLIBDIR%" -jar "%JAR%" %*

