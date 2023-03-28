@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug --remote P -P libraries/publisher.json -S libraries/subscriber-one.json -F output/082-23_Navigator-remote.log

cd /d "%base%"

