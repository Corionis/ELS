@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug --hints "libraries\Hint Server.json" -k "system\hint.keys" -O --remote P -P "libraries\Publisher.json" -s "libraries\Subscriber One.json" -F output\092-26_Navigator-remote.log

cd /d "%base%"

