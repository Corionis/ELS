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

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/082-21_Subscriber-listener.log

