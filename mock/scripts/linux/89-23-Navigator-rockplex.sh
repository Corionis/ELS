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

java -jar ../deploy/ELS.jar --navigator -c debug -d debug --remote P -p test/publisher/publisher.json -s rockplex.json -F output/89-23_Navigator-remote.log

