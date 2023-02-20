@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --ip-whitelist system/whitelist.txt --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/20-41_Subscriber-listener.log

