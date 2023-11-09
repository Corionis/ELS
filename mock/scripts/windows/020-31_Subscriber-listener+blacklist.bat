@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug --blacklist system/blacklist.txt --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/020-31_Subscriber-listener+blacklist.log

cd /d "%base%"

