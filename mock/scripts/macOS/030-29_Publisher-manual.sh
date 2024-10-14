#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote M -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -F output/030-29_Publisher-manual.log

