#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -P "libraries/Subscriber One.json" -S "libraries/Publisher.json" -F output/100-27_Navigator-sub-remote-hints.log

