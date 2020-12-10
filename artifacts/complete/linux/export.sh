#!/bin/bash

# Run ELS as a stand-alone local export-text and export-items process
#
# Use -d to add a date/time on the end of output filenames.
#
# This script may be executed from a file browser.
# All logging, text and JSON files are written to the ../output directory.
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

java -jar ${base}/../ELS.jar -d debug --dry-run -p ../meta/publisher.json -e ../output/${name}-text${stamp}.txt -i ../output/${name}-collection${stamp}.json -f ../output/${name}${stamp}.log

