#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --listener-quit -p libraries/publisher.json -s libraries/subscriber-one.json -F output/020-89_Quit-subscriber-listener.log

