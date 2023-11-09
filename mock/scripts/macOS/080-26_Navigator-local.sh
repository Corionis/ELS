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

rt/Contents/Home/bin/java -jar 'bin/ELS.jar' -C . --navigator -c trace -d trace -P libraries/publisher.json -s libraries/subscriber-one.json -F output/080-26_Navigator-local.log

