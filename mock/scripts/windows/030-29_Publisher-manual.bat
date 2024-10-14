@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -O --remote M -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\030-29_Publisher-manual.log

cd /d "%base%"

