#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --hints libraries/hint-server.json -k system/hint.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/092-21_Subscriber-One-listener.log

