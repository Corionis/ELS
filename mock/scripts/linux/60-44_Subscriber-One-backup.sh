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

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -p test/subscriber-one/subscriber-one.json -s test/publisher/publisher.json -T -m output/60-44_Subscriber-One-backup_mismatches.txt -W output/60-44_Subscriber-One-backup_whatsnew.txt -F output/60-44_Subscriber-One-backup.log

