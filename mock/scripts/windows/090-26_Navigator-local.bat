@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug --hints libraries/hint-server.json -k system/hint.keys -P libraries/publisher.json -s libraries/subscriber-one.json -F output/090-26_Navigator-local.log

cd /d "%base%"

