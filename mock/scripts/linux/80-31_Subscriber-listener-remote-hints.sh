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

java -jar ../deploy/ELS.jar -c debug -d debug -k test/test-hints.keys --remote S -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/80-31_Subscriber-listener-remomte-hints.log

