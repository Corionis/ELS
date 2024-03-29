@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug --hint-server libraries\hint-server.json -k system\hint.keys -P libraries\subscriber-one.json -S libraries\publisher.json -F output\100-27_Navigator-sub-remote-hints.log

cd /d "%base%"

