#!/bin/bash

# Run ELS as a stand-alone local dry run process
#
# Use -d to add a date/time on the end of output filenames.
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

if [ ! -d ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

# This is the same as the backup.bat with the addition of --dry-run
java -jar ${base}/../ELS.jar -d debug -p ../meta/publisher.json -s  ../meta/subscriber.json -T ../meta/subscriber-targets.json -m ../output/${name}-Mismatches${stamp}.txt -W ../output/${name}-WhatsNew${stamp}.txt -f ../output/${name}${stamp}.log --dry-run

