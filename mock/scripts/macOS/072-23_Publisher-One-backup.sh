#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -J --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug -O --remote P -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/072-23_Publisher-One-backup_mismatches.txt -W output/072-23_Publisher-One-backup_whatsnew.txt -F output/072-23_Publisher-One-backup.log

