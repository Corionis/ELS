#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -O --remote P -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/062-23_Publisher-One-backup_mismatches.txt -W output/062-23_Publisher-One-backup_whatsnew.txt -F output/062-23_Publisher-One-backup.log

