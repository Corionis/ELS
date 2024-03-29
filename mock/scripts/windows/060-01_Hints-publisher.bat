@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints libraries\hint-server.json --keys-only system\hint.keys -c debug -d debug -p libraries\publisher.json -T -F output\060-01_Hints-publisher.log

cd /d "%base%"

