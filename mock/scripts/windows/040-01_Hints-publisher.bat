@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -K system/hint.keys -c debug -d debug -p libraries/publisher.json -T -F output/040-01_Hints-publisher.log

cd /d "%base%"

