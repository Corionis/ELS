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

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote P --listener-keep-going -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/subscriber-one-targets.json -m output/020-24_Publisher-backup-keepgoing_mismatches.txt -W output/020-24_Publisher-backup-keepgoing_whatsnew.txt -F output/020-24_Publisher-keepgoing-backup.log

