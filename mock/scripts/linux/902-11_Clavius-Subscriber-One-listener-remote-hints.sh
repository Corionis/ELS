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

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hint-server private/hint-server-Win8Pro-64T.json -k system/hint.keys --remote S -p libraries/publisher.json -S private/clavius-subscriber-one.json -T -F output/902-11_Clavius-Subscriber-One-listener-remote-hints.log

