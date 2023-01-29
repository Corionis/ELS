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

java -jar ../deploy/ELS.jar --hint-server test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -p test/publisher/publisher.json -S test/subscriber-two/subscriber-two.json -T -m output/70-33_Publisher-Two-backup_mismatches.txt -W output/70-33_Publisher-Two-bacup_whatsnew.txt -F output/70-33_Publisher-Two-backup.log
