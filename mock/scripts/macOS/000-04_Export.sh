#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p "libraries/Publisher.json" -e output/000-04_Export.txt -i output/000-04_Export_collection.json -F output/000-04_Export.log

