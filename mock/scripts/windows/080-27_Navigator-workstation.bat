@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . --navigator -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -F output/080-27_Navigator-workstation.log

cd /d "%base%"

