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

java -jar bin/ELS.jar -C . -c debug -d debug --blacklist system/blacklist.txt --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/020-31_Subscriber-listener+blacklist.log
