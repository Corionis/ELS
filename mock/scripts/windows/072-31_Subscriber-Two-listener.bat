@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -J --hint-server libraries\hint-server.json -k system\hint.keys -c debug -d debug -O --remote S --authorize sharkbait -p libraries\publisher.json -S libraries\subscriber-two.json -T -F output\072-31_Subscriber-Two-listener.log

cd /d "%base%"

