#!/bin/bash

# Run ELS as a remote publisher dry run process
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

if [ ! -e ../output ]; then
    mkdir ../output
fi

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

# This is the same as the publisher-backup.bat with the addition of --dry-run
java -jar ${base}/../ELS.jar -d debug --dry-run --remote P -p ../meta/publisher.json -s  ../meta/subscriber.json -t ../meta/targets.json -m ../output/${name}-Mismatches.txt -n ../output/${name}-WhatsNew.txt -f ../output/${name}.log
