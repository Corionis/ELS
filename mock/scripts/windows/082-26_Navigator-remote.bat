@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . --navigator -c debug -d debug --remote P -P libraries/publisher.json -S libraries/subscriber-one.json -F output/082-26_Navigator-remote.log

cd /d "%base%"

