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

if [ -e ../output/${name}.log ]; then
    rm -f ../output/${name}.log
fi

java -jar ../deploy/ELS.jar -c debug -d debug --remote P -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -m output/${name}_mismatches.txt -W output/${name}_whatsnew.txt -F output/${name}.log --dry-run

