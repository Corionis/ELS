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

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug --hint-server "libraries/Hint Server.json" -K "system/hint.keys" -A "system/authentication.keys" -F output/102-10_Status-Server-listener.log
