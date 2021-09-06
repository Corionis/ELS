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

java -jar ../deploy/ELS.jar -c debug -d debug --remote P -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -m output/20-22_Publisher-dryrun_mismatches.txt -W output/20-22_Publisher-dryrun_whatsnew.txt -F output/20-22_Publisher-dryrun.log --dry-run
