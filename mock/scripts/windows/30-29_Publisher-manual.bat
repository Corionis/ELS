@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --remote M -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/30-29_Publisher-manual.log

