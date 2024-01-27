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

rt/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug --hints libraries/hint-server.json -k system/hint.keys -P libraries/subscriber-one.json -s libraries/publisher.json -F output/090-27_Navigator-sub-local.log

