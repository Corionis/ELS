@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote S --authorize sharkbait -p libraries/publisher.json -S libraries/subscriber-one.json -T -F output/62-21_Subscriber-One-listener.log

