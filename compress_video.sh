#!bash
# Shell script to reduce the size of videos without lowering the quality too much (hopefully)
# All of the work is done by ffmpeg, which must be in your PATH.

in="$1"
defout="${in%.*}.mp4"
out="${2-$defout}"

if [ ! -f "$in" ]
then
    echo "Usage : <infile> [<outfile>]" >&2
    [ "$in" != "" ] && echo "File not found: $in" >&2
    exit 1
fi

# Both of these commands work. The first runs a bit faster but produces a larger file
ffmpeg -i "$in" -y -nostdin -preset fast -vcodec h264 -acodec mp3 -hide_banner -nostats -v 0 "$out"
#ffmpeg -i "$in" -y -nostdin -crf 23 -hide_banner -nostats -v 0 "$out"
