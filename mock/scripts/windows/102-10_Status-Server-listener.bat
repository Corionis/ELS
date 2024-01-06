@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --hint-server libraries/hint-server.json -K system/hint.keys -A system/authentication.keys -F output/102-10_Status-Server-listener.log

cd /d "%base%"

