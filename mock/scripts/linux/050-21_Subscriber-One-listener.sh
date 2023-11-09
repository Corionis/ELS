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

rt/bin/java -jar bin/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote S --authorize sharkbait -p libraries/publisher.json -S libraries/subscriber-one.json -T -F output/050-21_Subscriber-One-listener.log
