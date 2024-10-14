#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug -O --remote P -P "libraries/Publisher.json" -S "libraries/Subscriber One.json" -F output/082-26_Navigator-remote.log

