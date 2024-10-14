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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/010-24_Backup-exclude-lib_mismatches.txt -W output/010-24_Backup-exclude-lib_whatsnew.txt -F output/010-24_Backup-exclude-lib.log -L "TV Shows"
