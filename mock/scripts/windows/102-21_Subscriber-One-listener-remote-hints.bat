@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -k system/hint.keys -A system/authentication.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/102-21_Subscriber-listener-remomte-hints.log

cd /d "%base%"

