#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --blacklist system/blacklist.txt --remote S -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -F output/020-31_Subscriber-listener+blacklist.log

