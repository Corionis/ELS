@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -K system/hint.keys -F output/072-10_Status-Server-listener.log

cd /d "%base%"

