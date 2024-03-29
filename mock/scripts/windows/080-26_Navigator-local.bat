@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar 'bin\ELS.jar' -C . --navigator -c trace -d trace -P libraries\publisher.json -s libraries\subscriber-one.json -F output\080-26_Navigator-local.log

cd /d "%base%"

