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

java -jar ../deploy/ELS.jar -c debug -d debug --listener-quit -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/20-99_Quit-subscriber-listener.log

