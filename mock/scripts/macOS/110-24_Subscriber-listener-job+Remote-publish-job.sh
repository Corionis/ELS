#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -j "503 Sub+Pub" -F "output/110-24_Subscriber-listener-job+Remote-publish-job.log"

