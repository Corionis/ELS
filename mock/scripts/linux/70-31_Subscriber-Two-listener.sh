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

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug --remote S --authorize sharkbait -p test/publisher/publisher.json -S test/subscriber-two/subscriber-two.json -T -F output/70-31_Subscriber-Two-listener.log
