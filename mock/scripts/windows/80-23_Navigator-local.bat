@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug -P libraries/publisher.json -s libraries/subscriber-one.json -F output/80-23_Navigator-local.log

