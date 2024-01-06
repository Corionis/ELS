@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --force-quit --hint-server libraries/hint-server.json -p libraries/publisher.json -F output/070-99_Quit-Status-Server.log

cd /d "%base%"

