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

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -m output/60-22_Publisher-One-dryrun_mismatches.txt -W output/60-22_Publisher-One-dryrun_whatsnew.txt -F output/60-22_Publisher-One-dryrun.log --dry-run
