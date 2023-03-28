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

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/050-22_Publisher-One-dryrun_mismatches.txt -W output/050-22_Publisher-One-dryrun_whatsnew.txt -F output/050-22_Publisher-One-dryrun.log --dry-run
