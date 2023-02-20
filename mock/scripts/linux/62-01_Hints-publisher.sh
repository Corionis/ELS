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

java -jar ../deploy/ELS.jar -C . --hints libraries/hint-server.json --keys-only system/hint.keys -c debug -d debug -p libraries/publisher.json -T -F output/62-01_Hints-publisher.log

