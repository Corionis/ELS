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

rt/bin/java -jar bin/ELS.jar -C . --hints "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/060-23_Publisher-One-backup_mismatches.txt -W output/060-23_Publisher-One-backup_whatsnew.txt -F output/060-23_Publisher-One-backup.log
