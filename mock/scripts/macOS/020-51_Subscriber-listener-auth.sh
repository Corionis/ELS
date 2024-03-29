#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --auth-keys system/authentication.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/020-51_Subscriber-listener-auth.log

