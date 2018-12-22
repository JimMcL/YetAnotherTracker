@ECHO OFF
@REM Simple wrapper to run the app.
@REM All command line arguments are just passed to the app.
@REM
@REM System requirements:
@REM Java
@REM OpenCV 3.2
@REM


@REM The jar file includes the 64 bit opencv DLL, opencv_java342.dll, which seems to include ffmpeg

@REM --- Configurable variables ---

@REM openCV location
set OCVD=C:\Jim\products\OpenCV-342\build\java
set CP=%OCVD%\opencv-342.jar

@REM Java location
@REM set JAVA=C:\Program Files\Java\jre-10.0.2\bin\java.exe
@REM set JAVA=C:\Program Files\Java\jdk-11.0.1\bin\java.exe
set JAVA=java

@REM --- Configurable variables ---

@REM This should not need to be edited.
@REM FFMPEG location
@REM Add the location of the FFMPEG DLL to the PATH environment variable. This is required to e.g. read mp4
@REM set PATH=%PATH%;%OCVD%\x64


set OPENCVLIBDIR=%OCVD%\x64
@REM library for writing h264 video (maybe not used?)
set OPENH264_LIBRARY_PATH=C:\Jim\products\openh264\openh264-1.6.0-win64msvc.dll

set YAT=%~dp0
set JAR=%YAT%\out\artifacts\YetAnotherTracker_jar\YetAnotherTracker.jar

"%JAVA%" "-Djava.library.path=%OPENCVLIBDIR%" -classpath "%CP%" -jar "%JAR%" %*

