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

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug -Y -F output/130-02_InstallUpdate.log $*
echo "Status: $?"

