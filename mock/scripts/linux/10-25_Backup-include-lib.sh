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

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/targets.json -m output/10-25_Backup-include-lib_mismatches.txt -W output/10-25_Backup-include-lib_whatsnew.txt -F output/10-25_Backup-include-lib.log -l Movies
