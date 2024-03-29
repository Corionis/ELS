#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -K system/hint.keys -A system/authentication.keys -F output/102-10_Status-Server-listener.log

