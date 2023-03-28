@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -k system/hint.keys --remote S --authorize sharkbait -p libraries/publisher.json -S libraries/subscriber-one.json -T -F output/072-21_Subscriber-One-listener.log

cd /d "%base%"

