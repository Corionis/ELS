@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug --remote S --authorize sharkbait -p libraries/publisher.json -S libraries/subscriber-two.json -T -F output/062-31_Subscriber-Two-listener.log

cd /d "%base%"

