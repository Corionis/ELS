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

java -jar ../deploy/ELS.jar -c debug -d debug -j "50 Any Renamer Tests" --remote J -p test/publisher/publisher.json -S test/subscriber-one/subscriber-one.json -F output/110-24_Publisher-any-publisher-job.log

