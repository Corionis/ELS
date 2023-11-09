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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/subscriber-one-targets.json -m output/010-22_Backup-dryrun_mismatches.txt -W output/010-22_Backup-dryrun_whatsnew.txt -F output/010-22_Backup-dryrun.log --dry-run
