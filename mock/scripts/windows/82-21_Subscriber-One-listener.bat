@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/82-21_Subscriber-listener.log

cd /d "%base%"

