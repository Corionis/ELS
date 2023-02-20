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

java -jar ../deploy/ELS.jar -C . -c debug -d debug --ip-whitelist system/whitelist.txt --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/20-41_Subscriber-listener.log

