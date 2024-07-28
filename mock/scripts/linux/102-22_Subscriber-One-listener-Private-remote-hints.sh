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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -J --hint-server private/hint-server.json -k system/hint.keys -A system/authentication.keys --listener-keep-going --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/102-21_Subscriber-listener-remomte-hints.log

