#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -m output/010-25_Backup-include-lib_mismatches.txt -W output/010-25_Backup-include-lib_whatsnew.txt -F output/010-25_Backup-include-lib.log -l Movies

