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

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug --remote P -P libraries/publisher.json -S libraries/subscriber-one.json -F output/082-23_Navigator-remote.log

