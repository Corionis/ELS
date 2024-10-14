@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -O --remote L --authorize sharkbait -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\030-31_Publisher-listener.log

cd /d "%base%"

