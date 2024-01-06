@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug --hint-server libraries/hint-server.json -k system/hint.keys --remote P -P libraries/publisher.json -s libraries/subscriber-one.json -F output/102-26_Navigator-remote-hints.log

cd /d "%base%"

