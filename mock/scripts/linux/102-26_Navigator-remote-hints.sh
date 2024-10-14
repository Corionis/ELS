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

rt/bin/java -jar bin/ELS.jar -C . --navigator -c trace -d trace -J --hint-server "libraries/Hint Server.json" -k "system/hint.keys" --remote P -P "libraries/Publisher.json" -O -s "libraries/Subscriber One.json" -F output/102-26_Navigator-remote-hints.log

