@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --navigator -c debug -d debug --hints test/hints/hint-server.json -k test/test-hints.keys -P test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/90-23_Navigator-local.log

