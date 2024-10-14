@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --auth-keys "system\authentication.keys" -O --remote S -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\020-51_Subscriber-listener-auth.log

cd /d "%base%"

