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

rt/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/060-22_Publisher-One-dryrun_mismatches.txt -W output/060-22_Publisher-One-dryrun_whatsnew.txt -F output/060-22_Publisher-One-dryrun.log --dry-run
