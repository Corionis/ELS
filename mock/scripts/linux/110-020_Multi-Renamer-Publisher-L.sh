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

rt/bin/java -jar bin/ELS.jar -C . -c info -d debug -p "libraries/Publisher.json" -j "020 Multi-Renamer Publisher L" -F "output/020 Multi-Renamer Publisher L.log"

