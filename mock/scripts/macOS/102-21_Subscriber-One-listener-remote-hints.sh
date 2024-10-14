#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -k "system/hint.keys" -A "system/authentication.keys" --remote S -p "libraries/Publisher.json" -s "libraries/Subscriber One.json" -T -F output/102-21_Subscriber-listener-remomte-hints.log

