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

java -jar ../deploy/ELS.jar -c debug -d debug --force-quit --hint-server test/hints/hint-server.json -p test/publisher/publisher.json -F output/72-99_Quit-Status-Server.log
