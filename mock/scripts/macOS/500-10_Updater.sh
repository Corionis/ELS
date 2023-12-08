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

rt/Contents/Home/bin/java -Dlog4j.configurationFile=../lib/log4j2-updater.xml -jar bin/ELS_Updater.jar -C .

