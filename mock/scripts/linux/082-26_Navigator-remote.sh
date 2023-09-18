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

java -jar bin/ELS.jar -C . --navigator -c debug -d debug --remote P -P libraries/publisher.json -S libraries/subscriber-one.json -F output/082-26_Navigator-remote.log

