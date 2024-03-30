#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c trace -d trace --hints libraries/hint-server.json -k system/hint.keys -P libraries/publisher.json -s libraries/subscriber-one.json -F output/090-26_Navigator-pub-local.log

