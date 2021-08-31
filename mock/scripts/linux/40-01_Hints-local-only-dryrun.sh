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

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ../deploy/ELS.jar -K test/test-hints.keys -c debug -d debug -p test/publisher/publisher.json -T -F output/${name}.log --dry-run

