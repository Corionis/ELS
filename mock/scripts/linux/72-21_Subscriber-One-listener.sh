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

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -k test/test-hints.keys --remote S --authorize sharkbait -p test/publisher/publisher.json -S test/subscriber-one/subscriber-one.json -T -F output/72-21_Subscriber-One-listener.log

