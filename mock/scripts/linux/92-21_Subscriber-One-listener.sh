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

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hints libraries/hint-server.json -k system/hint.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/92-21_Subscriber-One-listener.log

