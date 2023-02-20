#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

name=`basename $0 .sh`

cd ../..


if [ ! -d output ]; then
    mkdir output
fi

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/40-22_Publisher-dryrun_mismatches.txt -W output/40-22_Publisher-dryrun_whatsnew.txt -F output/40-22_Publisher-dryrun.log --dry-run
