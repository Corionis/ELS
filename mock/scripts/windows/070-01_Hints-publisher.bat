@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . --hints libraries/hint-server.json --keys-only system/hint.keys -c debug -d debug -p libraries/publisher.json -T -F output/070-01_Hints-publisher.log

cd /d "%base%"

