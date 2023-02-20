#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

name=`basename $0 .sh`

${base}/rt/bin/java -jar ${base}/bin/ELS.jar -n -c Debug -d Debug -F output/Navigator.log

