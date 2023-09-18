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

java -jar bin/ELS.jar -C . -c debug -d debug --remote T -p libraries/publisher.json -s libraries/subscriber-one.json -F output/030-39_Subscriber-terminal.log

