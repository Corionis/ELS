@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -k "system\hint.keys" -c debug -d debug -O --remote S --authorize sharkbait -p "libraries\Publisher.json" -S "libraries\Subscriber Two.json" -T -F output\072-31_Subscriber-Two-listener.log

cd /d "%base%"

