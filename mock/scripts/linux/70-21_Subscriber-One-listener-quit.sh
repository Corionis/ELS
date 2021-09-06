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

java -jar ../deploy/ELS.jar --quit-status --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug --remote S --authorize sharkbait -p test/publisher/publisher.json -S test/subscriber-one/subscriber-one.json -T -F output/70-21_Subscriber-One-listener.log
