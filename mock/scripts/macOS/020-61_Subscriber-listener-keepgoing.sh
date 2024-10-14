#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote S --listener-keep-going -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -F output/020-61_Subscriber-listener-keepgoing.log

