#!/bin/bash

# Run ELS as a remote publisher listener process
#
# Use -d to add a date/time on the end of output filenames.
#
# Run this before any remote subscriber process.
#
# Requests new collection and targets files from the subscriber.
# This allows the subscriber to make changes without sending those
# to the publisher separately.
#
# This script may be executed from a file browser.
# All logging is written to the ../output directory.
# Any existing log file is deleted first.

base=`dirname $0`
cd "$base"

name=`basename $0 .sh`

stamp=""
if [ "X${1}" != "X" -a "$1" == "-d" ]; then
    stamp="_`date +%Y%m%d-%H%M%S`"
fi

if [ ! -d ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ${base}/../ELS.jar -d debug --remote L --authorize passw0rd -p ../meta/publisher.json -S  ../meta/subscriber.json -T ../meta/subscriber-targets.json -f ../output/${name}${stamp}.log

