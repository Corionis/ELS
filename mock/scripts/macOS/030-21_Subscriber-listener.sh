#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --remote S --authorize sharkbait -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -F output/030-21_Subscriber-listener.log

