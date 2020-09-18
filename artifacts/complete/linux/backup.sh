#!/bin/bash

# Run ELS as a stand-alone local back-up process
#
# This script may be executed from a file browser.
# All logging, Mismatches, and What's New files are written to the ../output directory.
# Any existing log file is deleted first.

base=`dirname $0`
cd "$base"

name=`basename $0 .sh`

if [ ! -e ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ${base}/../ELS.jar -d debug -p ../meta/publisher.json -s  ../meta/subscriber.json -T ../meta/targets.json -m ../output/${name}-Mismatches.txt -n ../output/${name}-WhatsNew.txt -f ../output/${name}.log
