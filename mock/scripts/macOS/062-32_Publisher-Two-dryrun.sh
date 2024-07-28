#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -O --remote P -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/062-32_Publisher-Two-dryrun_mismatches.txt -W output/062-32_Publisher-Two-dryrun_whatsnew.txt -F output/062-32_Publisher-Two-dryrun.log --dry-run

