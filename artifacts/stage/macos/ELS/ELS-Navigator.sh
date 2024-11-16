#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

name=`basename $0 .sh`

"${base}/rt/Contents/Home/bin/java" -jar "${base}/bin/ELS.jar" -C "${base}" -n -c Debug -d Debug -F "output/${name}.log"

