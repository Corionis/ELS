@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p libraries\publisher.json -F output\000-07_Empty-directories.log --empty-directories

cd /d "%base%"

