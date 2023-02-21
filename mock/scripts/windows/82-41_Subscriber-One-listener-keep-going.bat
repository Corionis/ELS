@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --remote S --listener-keep-going -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/82-41_Subscriber-listener-keepgoing.log

cd /d "%base%"

