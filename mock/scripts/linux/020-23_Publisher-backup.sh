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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote P -p "libraries/Publisher.json" -O -s "libraries/Subscriber One.json" -T -m output/020-23_Publisher-backup_mismatches.txt -W output/020-23_Publisher-backup_whatsnew.txt -F output/020-23_Publisher-backup.log

