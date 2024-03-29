@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -j "502 Remote Publish" -F "502 Remote Publish.log"

cd /d "%base%"

