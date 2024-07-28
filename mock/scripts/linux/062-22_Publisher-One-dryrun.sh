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

rt/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -r P -p libraries/publisher.json -O -s libraries/subscriber-one.json -T -m output/062-22_Publisher-One-dryrun_mismatches.txt -W output/062-22_Publisher-One-dryrun_whatsnew.txt -F output/062-22_Publisher-One-dryrun.log --dry-run

