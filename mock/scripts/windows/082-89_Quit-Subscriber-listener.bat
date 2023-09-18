@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . -c debug -d debug --listener-quit -p libraries/publisher.json -s libraries/subscriber-one.json -F output/082-89_Quit-subscriber-listener.log

cd /d "%base%"

