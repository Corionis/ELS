@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -k "system\hint.keys" -A "system\authentication.keys" -O --remote S -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\102-21_Subscriber-listener-remomte-hints.log

cd /d "%base%"

