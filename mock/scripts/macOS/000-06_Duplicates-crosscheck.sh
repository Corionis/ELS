#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p "libraries/Publisher.json" -F output/000-06_Duplicates-crosscheck.log --duplicates --cross-check

