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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -k "system/hint.keys" --remote S --authorize sharkbait -p "libraries/Publisher.json" -g -S "libraries/Subscriber One.json" -T -F output/072-21_Subscriber-One-listener.log

