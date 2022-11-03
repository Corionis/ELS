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

java -jar ../deploy/ELS.jar -c debug -d debug --remote P --listener-keep-going -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T test/subscriber-one/targets.json -m output/20-24_Publisher-backup-keepgoing_mismatches.txt -W output/20-24_Publisher-backup-keepgoing_whatsnew.txt -F output/20-24_Publisher-keepgoing-backup.log

