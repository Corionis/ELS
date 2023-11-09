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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/subscriber-one-targets.json -F output/000-02_Bad-arguments.log -a-bad-argument
