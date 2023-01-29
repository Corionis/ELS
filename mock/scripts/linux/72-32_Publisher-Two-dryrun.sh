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

java -jar ../deploy/ELS.jar --hint-server test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug --remote P -p test/publisher/publisher.json -s test/subscriber-two/subscriber-two.json -T -m output/72-32_Publisher-Two-dryrun_mismatches.txt -W output/72-32_Publisher-Two-dryrun_whatsnew.txt -F output/72-32_Publisher-Two-dryrun.log --dry-run
