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

java -jar bin/ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -k system/hint.keys -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/070-23_Publisher-One-backup_mismatches.txt -W output/070-23_Publisher-One-backup_whatsnew.txt -F output/070-23_Publisher-One-backup.log

