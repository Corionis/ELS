#!/bin/bash

base=`dirname $0`
if [ "$base" = "." ]; then
    base=$PWD
fi
cd "$base"

cd ../..

rt/Contents/Home/bin/java -jar bin/ELS.jar -C . -c debug -d debug --force-quit -J --hint-server "libraries/Hint Server.json" -p "libraries/Publisher.json" -F output/070-99_Quit-Status-Server.log

