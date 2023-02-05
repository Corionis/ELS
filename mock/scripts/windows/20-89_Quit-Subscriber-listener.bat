@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --listener-quit -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -F output/20-89_Quit-subscriber-listener.log

