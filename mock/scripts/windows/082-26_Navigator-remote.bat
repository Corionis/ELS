@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -O --remote P -P "libraries\Publisher.json" -S "libraries\Subscriber One.json" -F output\082-26_Navigator-remote.log

cd /d "%base%"

