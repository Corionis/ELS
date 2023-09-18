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

java -jar bin/ELS.jar -C . --navigator -c debug -d debug -k system/hint.keys -P libraries/publisher.json -s libraries/subscriber-one.json -F output/090-26_Navigator-hint-keys.log

