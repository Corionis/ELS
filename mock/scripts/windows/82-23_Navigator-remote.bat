@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --navigator -c debug -d debug --remote P -P test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/82-23_Navigator-remote.log

