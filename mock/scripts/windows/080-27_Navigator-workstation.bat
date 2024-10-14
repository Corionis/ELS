@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -F output\080-27_Navigator-workstation.log

cd /d "%base%"

