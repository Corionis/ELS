#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -p "libraries/Subscriber One.json" -s "libraries/Publisher.json" -T -m output/060-44_Subscriber-One-backup_mismatches.txt -W output/060-44_Subscriber-One-backup_whatsnew.txt -F output/060-44_Subscriber-One-backup.log

