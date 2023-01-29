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

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -K test/test-hints.keys -F output/72-10_Status-Server-listener.log
