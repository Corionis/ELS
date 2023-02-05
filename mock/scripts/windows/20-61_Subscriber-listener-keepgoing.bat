@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --remote S --listener-keep-going -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/20-61_Subscriber-listener-keepgoing.log

