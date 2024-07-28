@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -J --hint-server libraries\hint-server.json -k system\hint.keys -P libraries\publisher.json -S libraries\subscriber-one.json -F output\100-26_Navigator-pub-remote-hints.log

cd /d "%base%"

