@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -J --hint-server libraries\hint-server.json -K system\hint.keys -A system\authentication.keys -F output\100-10_Status-Server-listener.log

cd /d "%base%"

