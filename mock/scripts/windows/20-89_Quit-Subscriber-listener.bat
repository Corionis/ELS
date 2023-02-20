@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --listener-quit -p libraries/publisher.json -s libraries/subscriber-one.json -F output/20-89_Quit-subscriber-listener.log

