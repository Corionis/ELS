@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . -c debug -d debug --remote M -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/030-29_Publisher-manual.log

cd /d "%base%"

