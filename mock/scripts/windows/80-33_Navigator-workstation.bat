@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -F output/80-33_Navigator-workstation.log

