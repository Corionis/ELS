#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -c debug -d debug -p "libraries/Publisher.json" -s "libraries/Subscriber Two.json" -T -m output/070-32_Publisher-Two-dryrun_mismatches.txt -W output/070-32_Publisher-Two-dryrun_whatsnew.txt -F output/070-32_Publisher-Two-dryrun.log --dry-run

