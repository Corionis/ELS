#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -O --remote P -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/020-22_Publisher-dryrun_mismatches.txt -W output/020-22_Publisher-dryrun_whatsnew.txt -F output/020-22_Publisher-dryrun.log --dry-run

