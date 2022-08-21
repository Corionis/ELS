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

java -jar ../deploy/ELS.jar --navigator -c debug -d debug --remote P -P test/workstation.json -s test/publisher/publisher.json -F output/80-43_Navigator-workstation.log

