@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote S --authorize sharkbait -p libraries/publisher.json -S libraries/subscriber-two.json -T -F output/50-31_Subscriber-Two-listener.log

