@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c trace -d trace --hints "libraries\Hint Server.json" -k "system\hint.keys" -P "libraries\Publisher.json" -s "libraries\Subscriber One.json" -F output\090-26_Navigator-pub-local.log

cd /d "%base%"

