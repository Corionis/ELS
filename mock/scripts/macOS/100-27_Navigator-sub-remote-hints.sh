#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug --hint-server libraries/hint-server.json -k system/hint.keys -P libraries/subscriber-one.json -S libraries/publisher.json -F output/100-27_Navigator-sub-remote-hints.log

