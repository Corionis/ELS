#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar 'bin/ELS.jar' -C . --navigator -c trace -d trace -P "libraries/Publisher.json" -s "libraries/Subscriber One.json" -F output/080-26_Navigator-local.log

