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

java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/subscriber-one.json -s libraries/publisher.json -T -m output/060-44_Subscriber-One-backup_mismatches.txt -W output/060-44_Subscriber-One-backup_whatsnew.txt -F output/060-44_Subscriber-One-backup.log

