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

rt/bin/java -jar bin/ELS.jar -C . -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -p "libraries/Publisher.json" -S "libraries/Subscriber Two.json" -T -m output/070-33_Publisher-Two-backup_mismatches.txt -W output/070-33_Publisher-Two-bacup_whatsnew.txt -F output/070-33_Publisher-Two-backup.log
