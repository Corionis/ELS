#!/bin/bash

# Run ELS as a remote subscriber terminal (interactive) process
#
# NOTE: Publisher and Subscriber are reversed!
#
# Use -d to add a date/time on the end of output filenames.
#
# Run publisher-listener.bat first.
#
# Forces the remote publisher to get new collection and targets files.
# This allows the subscriber to make changes without sending those
# to the publisher separately.
#
# This script may be executed from a file browser.
# All logging, Mismatches, and What's New files are written to the ../output directory.
# Any existing log file is deleted first.

base=`dirname $0`
cd "$base"

name=`basename $0 .sh`

stamp=""
if [ "X${1}" != "X" -a "$1" == "-d" ]; then
    stamp="_`date +%Y%m%d-%H%m%S`"
fi

if [ ! -e ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ${base}/../ELS.jar -d debug --remote T -p ../meta/subscriber.json -S  ../meta/publisher.json -T ../meta/targets.json -f ../output/${name}${stamp}.log

