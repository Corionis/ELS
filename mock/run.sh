#!/bin/bash

base=`dirname $0`
cd ${base}

java -cp "../out/production/Main/:../lib:../lib/*" com.groksoft.volmunger.Main $*
