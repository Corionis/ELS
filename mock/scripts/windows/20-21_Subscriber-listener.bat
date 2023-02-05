@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --remote S -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/20-21_Subscriber-listener.log

