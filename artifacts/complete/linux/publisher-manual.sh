#!/bin/bash

# Run ELS as a remote publisher manual (interactive) process
#
# Use -d to add a date/time on the end of output filenames.
#
# Run subscriber-listener.bat first.
#
# Requests new collection and targets files from the subscriber.
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
    stamp="_`date +%Y%m%d-%H%M%S`"
fi

if [ ! -e ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ${base}/../ELS.jar -d debug --remote M -p ../meta/publisher.json -s  ../meta/subscriber.json -t ../meta/targets.json -f ../output/${name}${stamp}.log

