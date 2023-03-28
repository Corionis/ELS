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

java -jar ../deploy/ELS.jar -C . -c debug -d debug --auth-keys system/auth.keys --remote S -p libraries/publisher.json -s libraries/publisher.json -T -F output/082-31_Subscriber-listener-auth.log

