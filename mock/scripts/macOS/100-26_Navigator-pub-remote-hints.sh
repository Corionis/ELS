#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -P "libraries/Publisher.json" -S "libraries/Subscriber One.json" -F output/100-26_Navigator-pub-remote-hints.log

