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

java -jar bin/ELS.jar -C . -c debug -d debug --remote L --authorize sharkbait -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/030-31_Publisher-listener.log
