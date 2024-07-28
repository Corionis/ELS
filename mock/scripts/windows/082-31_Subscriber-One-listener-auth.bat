@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --auth-keys system\authentication.keys -O --remote S -p libraries\publisher.json -s libraries\subscriber-one.json -T -F output\082-31_Subscriber-listener-auth.log

cd /d "%base%"

