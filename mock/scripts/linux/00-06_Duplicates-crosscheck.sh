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

java -jar ../deploy/ELS.jar -c debug -d debug -p test/publisher/publisher.json -T test/subscriber-one/targets.json -F output/00-06_Duplicates-crosscheck.log --duplicates --cross-check
