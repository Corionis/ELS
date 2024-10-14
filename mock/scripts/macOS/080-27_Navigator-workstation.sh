#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --navigator -c debug -d debug -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -F output/080-27_Navigator-workstation.log

