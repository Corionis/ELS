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

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/060-32_Publisher-Two-dryrun_mismatches.txt -W output/060-32_Publisher-Two-dryrun_whatsnew.txt -F output/060-32_Publisher-Two-dryrun.log --dry-run
