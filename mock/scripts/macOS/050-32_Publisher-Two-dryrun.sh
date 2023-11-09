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

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -S libraries/subscriber-two.json -T -m output/050-32_Publisher-Two-dryrun_mismatches.txt -W output/050-32_Publisher-Two-dryrun_whatsnew.txt -F output/050-32_Publisher-Two-dryrun.log --dry-run
