@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

C:\Users\trh\Tools\ELS\rt\bin\java -jar bin/ELS.jar -C . --navigator -c debug -d debug -P libraries/publisher.json -s libraries/subscriber-one.json -F output/080-26_Navigator-local.log
pause
cd /d "%base%"

