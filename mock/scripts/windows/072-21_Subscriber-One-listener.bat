@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -J --hint-server libraries\hint-server.json -k system\hint.keys -O --remote S --authorize sharkbait -p libraries\publisher.json -S libraries\subscriber-one.json -T -F output\072-21_Subscriber-One-listener.log

cd /d "%base%"

