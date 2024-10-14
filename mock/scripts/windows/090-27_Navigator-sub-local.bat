@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug --hints "libraries\Hint Server.json" -k "system\hint.keys" -P "libraries\Subscriber One.json" -s "libraries\Publisher.json" -F output\090-27_Navigator-sub-local.log

cd /d "%base%"

