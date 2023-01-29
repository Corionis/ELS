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

java -jar ../deploy/ELS.jar --navigator -c debug -d debug --hints test/hints/hint-server.json -k test/test-hints.keys --remote P -P test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/92-23_Navigator.log

