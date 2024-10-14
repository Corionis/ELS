#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" -O --remote P -P "libraries/Publisher.json" -s "libraries/Subscriber One.json" -F output/102-26_Navigator-remote-hints.log

