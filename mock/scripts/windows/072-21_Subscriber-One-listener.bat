@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -k "system\hint.keys" -O --remote S --authorize sharkbait -p "libraries\Publisher.json" -S "libraries\Subscriber One.json" -T -F output\072-21_Subscriber-One-listener.log

cd /d "%base%"

