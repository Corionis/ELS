#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote T -p "libraries/Publisher.json" -O -s "libraries/Subscriber One.json" -F output/030-39_Subscriber-terminal.log

