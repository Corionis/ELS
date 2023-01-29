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

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json --keys-only test/test-hints.keys -c debug -d debug -p test/publisher/publisher.json -T -F output/62-01_Hints-publisher.log

