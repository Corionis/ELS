@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -O --remote T -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -F output\030-39_Subscriber-terminal.log

cd /d "%base%"

