#!/bin/bash

base=`dirname $0`
cd ${base}

java -cp "../out/production/VolMonger/:../lib:../lib/*" com.groksoft.VolMonger -t -d debug -p TestRun/publisher/publisher-collection.json -s TestRun/subscriber-1/subscriber-1-collection.json
