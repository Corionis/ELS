@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -p "libraries\Publisher.json" -F output\080-03_Navigator-publisher-only.log $*

cd /d "%base%"

