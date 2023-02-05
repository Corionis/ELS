@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --remote M -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/30-29_Publisher-manual.log

