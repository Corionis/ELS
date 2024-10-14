#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -O --remote P -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/072-22_Publisher-One-dryrun_mismatches.txt -W output/072-22_Publisher-One-dryrun_whatsnew.txt -F output/072-22_Publisher-One-dryrun.log --dry-run

