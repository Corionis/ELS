@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --remote L --authorize sharkbait -p libraries\publisher.json -s libraries\subscriber-one.json -T -F output\030-31_Publisher-listener.log

cd /d "%base%"

