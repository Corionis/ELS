#!/bin/bash

base=`dirname $0`
cd ${base}

java -cp "../out/production/VolMonger/:../lib:../lib/*" com.groksoft.VolMonger $*
