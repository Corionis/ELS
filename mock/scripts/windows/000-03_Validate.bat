@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -F output/00-03_Validate.log --validate

cd /d "%base%"

