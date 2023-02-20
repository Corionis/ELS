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

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -T libraries/targets.json -e output/00-04_Export.txt -i output/00-04_Export_collection.json -F output/00-04_Export.log
