#!/bin/bash

# Run ELS as the Hint Status Server
#
# Use -d to add a date/time on the end of output filenames.
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

java -jar ${base}/../ELS.jar -d debug --hint-server ../meta/hint-server.json  -k els-hints.keys -f ../output/${name}${stamp}.log

