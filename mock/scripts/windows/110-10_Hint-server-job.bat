@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -j "510 Hint Status Server" -F "510 Hint Status Server.log"

cd /d "%base%"

