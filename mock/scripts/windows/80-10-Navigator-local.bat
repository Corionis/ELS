@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --navigator -c debug -d debug -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/80-10_Navigator.log

