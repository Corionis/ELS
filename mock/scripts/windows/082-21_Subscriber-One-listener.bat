@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -O --remote S -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\082-21_Subscriber-listener.log

cd /d "%base%"

