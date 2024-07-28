@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -J --hint-server libraries\hint-server.json -k system\hint.keys -O --remote P -P libraries\publisher.json -s libraries\subscriber-one.json -F output\102-26_Navigator-remote-hints.log

cd /d "%base%"

