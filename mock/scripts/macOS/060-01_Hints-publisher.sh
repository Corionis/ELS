#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . --hints "libraries/Hint Server.json" --keys-only "system/hint.keys" -c debug -d debug -p "libraries/Publisher.json" -T -F output/060-01_Hints-publisher.log

