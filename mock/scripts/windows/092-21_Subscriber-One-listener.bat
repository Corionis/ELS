@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --hints libraries\hint-server.json -k system\hint.keys --remote S -p libraries\publisher.json -s libraries\subscriber-one.json -T -F output\092-21_Subscriber-One-listener.log

cd /d "%base%"

