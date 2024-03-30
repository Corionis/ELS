#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/060-33_Publisher-Two-backup_mismatches.txt -W output/060-33_Publisher-Two-backup_whatsnew.txt -F output/060-33_Publisher-Two-backup.log

