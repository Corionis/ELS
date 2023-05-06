@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug --hints libraries/hint-server.json -k system/hint.keys --remote P -P libraries/publisher.json -s libraries/subscriber-one.json -F output/092-26_Navigator-remote.log

cd /d "%base%"

