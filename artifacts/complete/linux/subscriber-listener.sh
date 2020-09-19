#!/bin/bash

# Run ELS as a remote subscriber listener process
#
# NOTE: Publisher and Subscriber are reversed!
#
# Use -d to add a date/time on the end of output filenames.
#
# Run this before any remote publisher process.
#
# Forces the remote publisher to get new collection and targets files.
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
    stamp="_`date +%Y%m%d-%H%m%S`"
fi

if [ ! -e ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ${base}/../ELS.jar -d debug --remote S -p ../meta/subscriber.json -s ../meta/publisher.json -T ../meta/publisher-targets.json -f ../output/${name}${stamp}.log

